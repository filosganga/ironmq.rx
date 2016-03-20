package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import GraphDSL.Implicits._

object Test2 extends App {

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val queue = "test"
  val clientProvider = () => new IronMqClient(as, IronMqSettings(as))

  val consumeFlow = Source.fromGraph(new IronMqGraphStageSource(queue, clientProvider))
    .to(Sink.foreach { msg =>
      println(msg)
    })


  val produceFlow = Source.tick(1.second, 10.millis, "Test")
    .to(Sink.fromGraph(new IronMqGraphStageSink(queue, clientProvider)))

  consumeFlow.run()
  produceFlow.run()

  Console.readLine()

  mat.shutdown()
  as.shutdown()

}
