package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Test extends App {

  val as = ActorSystem()
  val ironMqClient = new IronMqClient(as, IronMqSettings(as))

  val ids = Await.result(ironMqClient.pushMessages(
    "test",
    PushMessage("test-1"),
    PushMessage("test-2"),
    PushMessage("test-3"),
    PushMessage("test-4"),
    PushMessage("test-5")
  ), 15.seconds)

  println(ids)

  val messages = Await.result(for {
    xs <- ironMqClient.reserveMessages("test", noOfMessages = 100)
    _ <- ironMqClient.deleteMessages("test", xs.map(_.reservation))
  } yield xs.map(_.message), 15.seconds)

  println(messages)

  as.shutdown()
}
