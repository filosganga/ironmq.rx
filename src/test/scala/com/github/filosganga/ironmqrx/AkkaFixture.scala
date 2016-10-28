package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration._


trait AkkaFixture extends ConfigFixture with BeforeAndAfterAll {
  _: Suite =>

  implicit lazy val actorSystem: ActorSystem = ActorSystem(s"test-${System.currentTimeMillis()}", config)

  override protected def afterAll(){
    actorSystem.terminate()
    super.afterAll()
  }
}
