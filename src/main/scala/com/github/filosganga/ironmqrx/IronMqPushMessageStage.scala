package com.github.filosganga.ironmqrx

import akka.Done
import akka.stream._
import akka.stream.stage._

import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Failure, Success}

class IronMqPushMessageStage(queue: Queue.Name, clientProvider: () => IronMqClient) extends GraphStage[FlowShape[PushMessage, Message.Id]] {

  val in: Inlet[PushMessage] = Inlet("in")
  val out: Outlet[Message.Id] = Outlet("out")

  override val shape: FlowShape[PushMessage, Message.Id] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    implicit def ec: ExecutionContext = materializer.executionContext

    val client = clientProvider()

    // Need to be alive after the input port is closed to allow the last messages to be pushed
    setKeepGoing(true)

    setHandler(in, new InHandler {

      override def onPush() {
        val pushMessage = grab(in)
        val future = client.pushMessages(queue, pushMessage)

        future.onComplete {
          case Success(ids) =>
          case Failure(e) =>
        }
      }
    })

    val pushMessagesCallBack = getAsyncCallback { ids: Seq[Message.Id] =>
      emitMultiple()
    }

    setHandler(out, new OutHandler {
      override def onPull() {

      }
    })

    override def postStop(): Unit = {
      client.close()
    }
  }
}