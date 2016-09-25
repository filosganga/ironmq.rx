package com.github.filosganga.ironmqrx

import akka.stream.{Attributes, Inlet, SinkShape}
import akka.stream.stage.{InHandler, GraphStageLogic, GraphStage}

import scala.concurrent.ExecutionContext

class IronMqGraphStageSink(queue: String, clientProvider: () => IronMqClient) extends GraphStage[SinkShape[String]] {

  val messages: Inlet[String] = Inlet("messages")

  override val shape: SinkShape[String] = SinkShape(messages)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    implicit def ec: ExecutionContext = materializer.executionContext
    val client = clientProvider()

    setHandler(messages, new InHandler {

      override def onPush(): Unit = {
        val element = grab(messages)

        client.pushMessages(queue, PushMessage(element)).onFailure {
          case error => failStage(error)
        }

        pull(messages)
      }
    })

    override def postStop(): Unit = {
      client.close()
    }

    override def preStart(): Unit = {
      pull(messages)
    }
  }
}