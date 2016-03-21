package com.github.filosganga.ironmqrx

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.Xor
import com.github.filosganga.ironmqrx.Codec._
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.{ACursor, JsonObject, Json}
import io.circe.syntax._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object IronMqClient {

  def apply(actorSystem: ActorSystem): IronMqClient =
    apply(actorSystem, actorSystem.settings.config)

  def apply(actorSystem: ActorSystem, config: Config): IronMqClient =
    apply(actorSystem, IronMqSettings(config))

  def apply(actorSystem: ActorSystem, settings: IronMqSettings): IronMqClient =
    new IronMqClient(actorSystem, settings)

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

  def listQueues(prefix: Option[String] = None, from: Option[Queue.Name] = None, noOfQueues: Int = 50)(implicit ec: ExecutionContext): Future[Iterable[Queue.Name]] = {

    def parseQueues(json: Json) = {

      def extractName(json: Json) = json.hcursor.downField("name").as[Json].getOrElse(Json.empty)

      json.hcursor.downField("queues").withFocus { xsJson =>
        xsJson.mapArray { xs =>
          xs.map(extractName)
        }
      }.as[Iterable[Queue.Name]]
    }

    val query = List(prefix.map("prefix" -> _), from.map("previous" -> _.value))
      .collect {
        case Some(x) => x
      }
      .foldLeft(Uri.Query("per_page" -> noOfQueues.toString)) { (q, x) =>
        x +: q
      }

    makeRequest(Get(Uri(s"/3/projects/${settings.projectId}/queues").withQuery(query)))
      .flatMap(Unmarshal(_).to[Json])
      .map(parseQueues)
      .collect {
        case Xor.Right(xs) => xs
      }
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

    val payload = (if (timeout.isFinite()) {
      Json.obj("timeout" -> Json.long(timeout.toSeconds))
    } else {
      Json.empty
    }) deepMerge (if (watch.isFinite()) {
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

    val payload = (if (timeout.isFinite()) {
      Json.obj("timeout" -> Json.long(timeout.toSeconds))
    } else {
      Json.empty
    }) deepMerge (if (watch.isFinite()) {
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
