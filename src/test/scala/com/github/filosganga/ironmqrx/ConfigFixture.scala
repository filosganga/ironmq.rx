package com.github.filosganga.ironmqrx

import com.typesafe.config.ConfigFactory
import org.scalatest.Suite


trait ConfigFixture {
  _: Suite =>

  lazy val config = ConfigFactory.defaultReference()

}
