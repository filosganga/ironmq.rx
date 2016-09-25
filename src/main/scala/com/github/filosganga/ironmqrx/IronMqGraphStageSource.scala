package com.github.filosganga.ironmqrx

import akka.stream.stage._
import akka.stream._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object IronMqGraphStageSource {

  val FetchMessagesTimerKey = "fetch-messages"
  val DeleteMessagesTimerKey = "delete-messages"
}

class IronMqGraphStageSource(queue: Queue.Name, clientProvider: () => IronMqClient) extends GraphStage[SourceShape[Message]] {
  import IronMqGraphStageSource._

  val messages: Outlet[Message] = Outlet("messages")

  val minBufferSize = 25
  val maxBufferSize = 100

  val fetchInterval = 100.millis
  val deleteInterval = 10.seconds
  val reservationTimeout = 1.minute

  override def shape: SourceShape[Message] = SourceShape(messages)

  override def createLogic(inheritedAttributes: Attributes): TimerGraphStageLogic = {
    new TimerGraphStageLogic(shape) {

      implicit def ec = materializer.executionContext

      var fetching: Boolean = false
      var buffer: List[ReservedMessage] = List.empty
      var reservations: List[Reservation] = List.empty
      val client: IronMqClient = clientProvider()

      setHandler(messages, new OutHandler {

        override def onPull(): Unit = {

          if(!isTimerActive(FetchMessagesTimerKey)) {
            schedulePeriodically(FetchMessagesTimerKey, fetchInterval)
          }

          deliveryMessages()
        }

        override def onDownstreamFinish(): Unit = {
          releaseMessages().onComplete { _ =>
            client.close()
            super.onDownstreamFinish()
          }
        }
      })

      override protected def onTimer(timerKey: Any): Unit = timerKey match {

        case FetchMessagesTimerKey =>
          fetchMessages()

        case DeleteMessagesTimerKey =>
          deleteMessages()

      }

      override def postStop(): Unit = {
        client.close()
        super.postStop()
      }

      def fetchMessages(): Unit = {

        if (!fetching && buffer.size < minBufferSize) {
          fetching = true
          client.reserveMessages(queue, maxBufferSize - buffer.size, timeout = reservationTimeout).onComplete {
            case Success(xs) =>
              updateBuffer.invoke(xs.toList)
              updateFetching.invoke(false)
            case Failure(error) =>
              fail(messages, error)
              updateFetching.invoke(false)
          }
        }
      }

      def deleteMessages(): Unit = {
        val (toDelete, toKeep) = reservations.splitAt(100)
        client.deleteMessages(queue, toDelete).onComplete {
          case Success(_) =>
            updateReservations.invoke(toKeep)
          case Failure(error) =>
            fail(messages, error)
        }
      }

      def deliveryMessages(): Unit = {
        while(buffer.nonEmpty && isAvailable(messages)) {
          val messageToDelivery = buffer.head
          push(messages, messageToDelivery.message)
          reservations = messageToDelivery.reservation :: reservations
          buffer = buffer.tail
        }

        if(!isTimerActive(DeleteMessagesTimerKey)){
          schedulePeriodically(DeleteMessagesTimerKey, deleteInterval)
        }
      }

      def releaseMessages(): Future[Unit] = {
        Future.sequence(reservations.map(reservation => client.releaseMessage(queue, reservation))).map(_ => Unit)
      }

      val updateBuffer = getAsyncCallback { xs: List[ReservedMessage] =>
        buffer = buffer ::: xs
        deliveryMessages()
      }

      val updateFetching = getAsyncCallback { x: Boolean =>
        fetching = x
      }

      val updateReservations = getAsyncCallback { xs: List[Reservation] =>
        reservations = xs
      }

    }
  }

}
