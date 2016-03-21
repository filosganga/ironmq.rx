package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration._


trait AkkaFixture extends BeforeAndAfterAll {
  _: Suite =>

  private var unsafeActorSystem: ActorSystem = _
  implicit def actorSystem: ActorSystem = unsafeActorSystem

  override protected def beforeAll(){
    super.beforeAll()
    unsafeActorSystem = ActorSystem()
  }

  override protected def afterAll(){
    val actorSystemTermination = unsafeActorSystem.terminate()
    super.afterAll()
    Await.result(actorSystemTermination, 15.seconds)
  }
}
