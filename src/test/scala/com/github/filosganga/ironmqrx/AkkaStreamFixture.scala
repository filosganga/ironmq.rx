package com.github.filosganga.ironmqrx

import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterEach, Suite}

/**
  *
  */
trait AkkaStreamFixture extends AkkaFixture with BeforeAndAfterEach {
  _: Suite =>

  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  override protected def afterEach(): Unit = {
    materializer.shutdown()
    super.afterEach()
  }
}
