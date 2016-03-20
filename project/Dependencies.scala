import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._

object Dependencies {

  object typesafe {
    val config = "com.typesafe" % "config" % "1.3.0"
  }

  object scala {

    private val version = "2.11.7"

    val library = "org.scala-lang" % "scala-library" % version
    val reflect = "org.scala-lang" % "scala-reflect" % version

    object modules {

      private val version = "1.0.4"

      val xml = "org.scala-lang.modules" %% "scala-xml" % version
      val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % version
    }

  }

  object slf4j {

    private val version = "1.7.12"

    val api = "org.slf4j" % "slf4j-api" % version
    val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % version
    val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % version
  }

  object log4j {
    private val version = "2.5"

    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % version
  }

  object logback {

    private val version = "1.1.3"

    val core = "ch.qos.logback" % "logback-core" % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object akka {

    private val version = "2.4.2"

    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val remote = "com.typesafe.akka" %% "akka-remote" % version
    val cluster = "com.typesafe.akka" %% "akka-cluster" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    val contrib = "com.typesafe.akka" %% "akka-contrib" % version
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % version
    val clusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % version
    val clusterMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % version
    val persistence = "com.typesafe.akka" %% "akka-persistence-experimental" % version
    val persistenceTck = "com.typesafe.akka" %% "akka-persistence-experimental-tck" % version
    val testKit = "com.typesafe.akka" %% "akka-testkit" % version
    val stream = "com.typesafe.akka" %% "akka-stream" % version
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % version
    val httpCore = "com.typesafe.akka" %% "akka-http-core" % version
    val http = "com.typesafe.akka" %% "akka-http-experimental" % version
    val httpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % version


    object httpJson {

      private val version = "1.5.2"

      val play = "de.heikoseeberger" %% "akka-http-play-json" % version
      val json4s = "de.heikoseeberger" %% "akka-http-json4s" % version
      val argonaut = "de.heikoseeberger" %% "akka-http-argonaut" % version
      val upickle = "de.heikoseeberger" %% "akka-http-upickle" % version
      val circe = "de.heikoseeberger" %% "akka-http-circe" % version
    }

  }


  object circe {

    private val version = "0.3.0"

    val core = "io.circe" %% "circe-core" % version
    val generic = "io.circe" %% "circe-generic" % version
    val parser = "io.circe" %% "circe-parser" % version

  }

  object spire {

    private val version = "0.11.0"

    val core = "org.spire-math" %% "spire" % version
    val macros = "org.spire-math" %% "spire-macros" % version
    val laws = "org.spire-math" %% "spire-laws" % version
    val extras = "org.spire-math" %% "spire-extras" % version


    object algebra {

      private val version = "0.3.1"

      val core = "org.spire-math" % "algebra" % version
      val std = "algebra-std" % "algebra-std" % version
      val laws = "algebra-std" % "algebra-laws" % version
    }

    object cats {

      private val version = "0.3.0"

      val all = "org.spire-math" %% "cats" % version
      val core = "org.spire-math" %% "cats-core" % version
      val macros = "org.spire-math" %% "cats-macros" % version
      val laws = "org.spire-math" %% "cats-laws" % version
      val free = "org.spire-math" %% "cats-free" % version
      val state = "org.spire-math" %% "cats-state" % version
    }

  }

  val simulacrum = "com.github.mpilquist" %% "simulacrum" % "0.7.0"

  object typelevel {
    val machinist = "org.typelevel" %% "machinist" % "0.4.1"
    val discipline = "org.typelevel" %% "discipline" % "0.4"
  }

  object monocle {

    private val version = "1.2.0"

    val core = "com.github.julien-truffaut" %% "monocle-core" % version
    val generic = "com.github.julien-truffaut" %% "monocle-generic" % version
    val makro = "com.github.julien-truffaut" %% "monocle-macro" % version
    val state = "com.github.julien-truffaut" %% "monocle-state" % version
    val refined = "com.github.julien-truffaut"  %%  "monocle-refined" % version
    val law = "com.github.julien-truffaut" %% "monocle-law" % version
  }

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.0"
  val scalaArm = "com.jsuereth" %% "scala-arm" % "1.4"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.5"
  val scopt = "com.github.scopt" %% "scopt" % "3.3.0"

  object scalaMock {

    private val version = "3.2"

    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % version
  }

  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.11.10"
  val spotifyDockerClient = "com.spotify" % "docker-client" % "3.5.9"
}