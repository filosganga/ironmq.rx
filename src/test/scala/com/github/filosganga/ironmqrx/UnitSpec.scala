package com.github.filosganga.ironmqrx

import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class UnitSpec extends WordSpec with Matchers with ScalaFutures {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 15.seconds, interval =  1.second)
}
