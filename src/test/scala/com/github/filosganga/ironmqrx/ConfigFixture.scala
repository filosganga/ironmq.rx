package com.github.filosganga.ironmqrx

import com.typesafe.config.ConfigFactory
import org.scalatest.Suite


trait ConfigFixture {
  _: Suite =>

  def config = ConfigFactory.defaultReference()

}
