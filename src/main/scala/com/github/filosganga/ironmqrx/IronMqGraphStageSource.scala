package com.github.filosganga.ironmqrx

import akka.stream.stage._
import akka.stream._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object IronMqGraphStageSource {

  val FetchMessagesTimerKey = "fetch-messages"
}

class IronMqGraphStageSource(queue: Queue.Name, clientProvider: () => IronMqClient) extends GraphStage[SourceShape[Message]] {
  import IronMqGraphStageSource._

  val messages: Outlet[Message] = Outlet("messages")

  val minBufferSize = 25
  val maxBufferSize = 100

  val fetchInterval = 150.millis
  val pullTimeout = 50.millis

  override def shape: SourceShape[Message] = SourceShape(messages)

  override def createLogic(inheritedAttributes: Attributes): TimerGraphStageLogic = {
    new TimerGraphStageLogic(shape) {

      implicit def ec = materializer.executionContext

      var fetching: Boolean = false
      var buffer: List[Message] = List.empty
      val client: IronMqClient = clientProvider()

      setHandler(messages, new OutHandler {

        override def onPull(): Unit = {

          if(!isTimerActive(FetchMessagesTimerKey)) {
            schedulePeriodically(FetchMessagesTimerKey, fetchInterval)
          }

          deliveryMessages()
        }
      })

      override protected def onTimer(timerKey: Any): Unit = timerKey match {

        case FetchMessagesTimerKey =>
          fetchMessages()

      }

      override def postStop(): Unit = {
        client.close()
        super.postStop()
      }

      def fetchMessages(): Unit = {

        if (!fetching && buffer.size < minBufferSize) {
          fetching = true
          client.pullMessages(queue, maxBufferSize - buffer.size, watch = pullTimeout).onComplete {
            case Success(xs) =>
              updateBuffer.invoke(xs.toList)
              updateFetching.invoke(false)
            case Failure(error) =>
              fail(messages, error)
              updateFetching.invoke(false)
          }
        }
      }

      def deliveryMessages(): Unit = {
        while(buffer.nonEmpty && isAvailable(messages)) {
          val messageToDelivery = buffer.head
          push(messages, messageToDelivery)
          buffer = buffer.tail
        }
      }

      val updateBuffer = getAsyncCallback { xs: List[Message] =>
        buffer = buffer ::: xs
        deliveryMessages()
      }

      val updateFetching = getAsyncCallback { x: Boolean =>
        fetching = x
      }

    }
  }

}
