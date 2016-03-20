package com.github.filosganga.ironmqrx

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest, Uri}
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, OAuth2BearerToken, Authorization}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import cats.data.Xor
import com.typesafe.config.Config
import io.circe.{Decoder, HCursor, Cursor, Json}
import org.reactivestreams.{Subscriber, Publisher}

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.client.RequestBuilding._
import Codec._
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.syntax._

import scala.util.{Failure, Success}

object IronMqSettings {
  def apply(config: Config): IronMqSettings =
    new IronMqSettings(config.getConfig("ironmq-rx"))

  def apply(as: ActorSystem): IronMqSettings =
    apply(as.settings.config)

}

class IronMqSettings(config: Config) {
  val host: String = config.getString("host")
  val projectId: String = config.getString("credentials.project-id")
  val token: String = config.getString("credentials.token")
}

class IronMqClient(actorSystem: ActorSystem, settings: IronMqSettings) extends AutoCloseable {

  private implicit val as: ActorSystem = actorSystem
  private implicit val materializer = ActorMaterializer()
  private val http = Http(actorSystem)

  private val pipeline: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest]
    .map(_.withHeaders(Authorization(GenericHttpCredentials("OAuth", settings.token))))
    .map(_ -> NotUsed)
    .via(http.cachedHostConnectionPoolHttps[NotUsed](settings.host))
    .map(_._1)
    .mapAsync(1) {
      case Success(response) if response.status.isSuccess() =>
        FastFuture.successful(response)
      case Success(response) if !response.status.isSuccess() =>
        FastFuture.failed(new RuntimeException(response.status.reason()))
      case Failure(error) =>
        FastFuture.failed(error)
    }

  def pushMessages(queue: String, messages: PushMessage*)(implicit ec: ExecutionContext): Future[Message.Ids] = {

    val payload = Json.obj("messages" -> Json.fromValues(
      messages.map { pm =>
        Json.obj("body" -> Json.string(pm.body), "delay" -> Json.long(pm.delay.toSeconds))
      }
    ))

    makeRequest(Post(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages"), payload))
      .flatMap(Unmarshal(_).to[Message.Ids])
  }

  def reserveMessages(
    queue: String,
    noOfMessages: Int = 1,
    timeout: Duration = Duration.Undefined,
    watch: Duration = Duration.Undefined
  )(implicit ec: ExecutionContext): Future[Iterable[ReservedMessage]] = {

    val payload = (if(timeout.isFinite()){
      Json.obj("timeout" -> Json.long(timeout.toSeconds))
    } else {
      Json.empty
    }) deepMerge (if(watch.isFinite()) {
      Json.obj("wait" -> Json.long(watch.toSeconds))
    } else {
      Json.empty
    }) deepMerge Json.obj("n" -> Json.int(noOfMessages), "delete" -> Json.bool(false))

    makeRequest(Post(Uri(s"/3/projects/${settings.projectId}/queues/$queue/reservations"), payload))
      .flatMap(Unmarshal(_).to[Json])
      .map { json =>
        json.hcursor.downField("messages").as[Iterable[ReservedMessage]]
      }
      .collect {
        case Xor.Right(xs) => xs
      }
  }

  def pullMessages(
    queue: String,
    noOfMessages: Int = 1,
    timeout: Duration = Duration.Undefined,
    watch: Duration = Duration.Undefined
  )(implicit ec: ExecutionContext): Future[Iterable[Message]] = {

    val payload = (if(timeout.isFinite()){
      Json.obj("timeout" -> Json.long(timeout.toSeconds))
    } else {
      Json.empty
    }) deepMerge (if(watch.isFinite()) {
      Json.obj("wait" -> Json.long(watch.toSeconds))
    } else {
      Json.empty
    }) deepMerge Json.obj("n" -> Json.int(noOfMessages), "delete" -> Json.bool(true))

    makeRequest(Post(Uri(s"/3/projects/${settings.projectId}/queues/$queue/reservations"), payload))
      .flatMap(Unmarshal(_).to[Json])
      .map { json =>
        json.hcursor.downField("messages").as[Iterable[Message]]
      }
      .collect {
        case Xor.Right(xs) => xs
      }
  }

  def touchMessage(
    queue: String,
    reservation: Reservation,
    timeout: Duration = Duration.Undefined
  )(implicit ec: ExecutionContext): Future[Reservation] = {

    val payload = (if (timeout.isFinite()) {
      Json.obj("timeout" -> timeout.toSeconds.asJson)
    } else {
      Json.empty
    }) deepMerge Json.obj("reservation_id" -> reservation.reservationId.asJson)

    makeRequest(Post(s"/3/projects/${settings.projectId}/queues/$queue/messages/${reservation.messageId}/touch", payload))
      .flatMap(Unmarshal(_).to[Json])
      .map { json =>
        for {
          reservationId <- json.hcursor.downField("reservation_id").as[Reservation.Id]
        } yield reservation.copy(reservationId = reservationId)
      }
      .collect {
        case Xor.Right(r) => r
      }

  }

  def peekMessages(
    queue: String,
    numberOfMessages: Int = 1
  )(implicit ec: ExecutionContext): Future[Iterable[Message]] = {

    val payload = Json.obj("n" -> numberOfMessages.asJson)

    makeRequest(Get(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages")
      .withQuery(Uri.Query("n" -> numberOfMessages.toString))))
      .flatMap(Unmarshal(_).to[Json])
      .map { json =>
        json.hcursor.downField("messages").as[Iterable[Message]]
      }
      .collect {
        case Xor.Right(xs) => xs
      }

  }

  def deleteMessage(queue: String, reservation: Reservation)(implicit ec: ExecutionContext): Future[Unit] = {

    val payload = reservation.asJson

    makeRequest(Delete(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages/${reservation.messageId}"), payload))
      .map(_ => Unit)

  }

  def deleteMessages(queue: String, reservations: Iterable[Reservation])(implicit ec: ExecutionContext): Future[Unit] = {

    val payload = Json.obj("ids" -> Json.fromValues(reservations.map(_.asJson).toSeq))

    makeRequest(Delete(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages"), payload))
      .map(_ => Unit)

  }

  def releaseMessage(
    queue: String,
    reservation: Reservation,
    delay: FiniteDuration = Duration.Zero
  )(implicit ec: ExecutionContext): Future[Unit] = {

    val payload = reservation.asJson deepMerge Json.obj("delay" -> delay.toSeconds.asJson)

    makeRequest(Post(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages"), payload))
      .map(_ => Unit)
  }

  def clearMessages(queue: String)(implicit ec: ExecutionContext): Future[Unit] = {
    makeRequest(Delete(Uri(s"/3/projects/${settings.projectId}/queues/$queue/messages"), Json.obj()))
      .map(_ => Unit)

  }

  private def makeRequest(request: HttpRequest): Future[HttpResponse] = {
    Source.single(request).via(pipeline).runWith(Sink.head)
  }

  override def close() {
    materializer.shutdown()
  }
}
