package com.github.filosganga.ironmqrx

import akka.stream.{Attributes, Inlet, SinkShape}
import akka.stream.stage.{InHandler, GraphStageLogic, GraphStage}

import scala.concurrent.ExecutionContext

class IronMqGraphStageSink(queue: String, clientProvider: () => IronMqClient) extends GraphStage[SinkShape[String]] {

  val client = clientProvider()
  val closeTimeoutMs = 1000L

  val messages: Inlet[String] = Inlet("messages")

  override val shape: SinkShape[String] = SinkShape(messages)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    implicit def ec: ExecutionContext = materializer.executionContext
    var closed = false

    setHandler(messages, new InHandler {
      override def onPush(): Unit = {
        val element = grab(messages)

        client.pushMessages(queue, PushMessage(element)).onFailure {
          case error => failStage(error)
        }

        pull(messages)
      }

      override def onUpstreamFinish(): Unit = {
        close()
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        close()
      }
    })

    def close(): Unit =
      if (!closed) {
        client.close()
        closed = true
      }

    override def afterPostStop(): Unit = {
      close()
      super.afterPostStop()
    }

    override def preStart(): Unit = {
      pull(messages)
    }
  }
}