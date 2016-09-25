package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration._


trait AkkaFixture extends ConfigFixture with BeforeAndAfterAll {
  _: Suite =>

  private var unsafeActorSystem: ActorSystem = _
  implicit def actorSystem: ActorSystem = unsafeActorSystem

  override protected def beforeAll(){
    super.beforeAll()
    unsafeActorSystem = ActorSystem(s"test-${System.currentTimeMillis()}", config)
  }

  override protected def afterAll(){
    unsafeActorSystem.terminate()
    super.afterAll()
  }
}
