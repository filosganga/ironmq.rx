import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._

object Versions {

  val typesafeConfig = "1.3.0"
  val akka = "2.4.9"
  val scalaTest = "3.0.0"
  val scalaCheck = "1.12.5"
  val scalaMock = "3.2"
  val akkaHttpJson = "1.9.0"
}

object Dependencies {

  object typesafe {
    val config = "com.typesafe" % "config" % Versions.typesafeConfig
  }

  object akka {
    val stream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka
    val httpCore = "com.typesafe.akka" %% "akka-http-core" % Versions.akka
    val http = "com.typesafe.akka" %% "akka-http-experimental" % Versions.akka
    val httpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Versions.akka


    object httpJson {
      val circe = "de.heikoseeberger" %% "akka-http-circe" % Versions.akkaHttpJson
    }
  }

  val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck

  object scalaMock {
    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % Versions.scalaMock
  }
}