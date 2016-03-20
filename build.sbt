import Dependencies._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._

lazy val ironmqrx = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
  organization := "com.filippodeluca",
  name := "ironmq-rx",
  homepage := Some(url("https://github.com/filosganga/ironmq.rx")),
  startYear := Some(2016),

  version := "1.0-SNAPSHOT",

  git.remoteRepo := "origin",
  git.runner := ConsoleGitRunner,
  git.baseVersion := "1.0",
  git.useGitDescribe := true,

  licenses := Seq(
    ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  ),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/filosganga/ironmq.rx"),
      "scm:git:https://github.com/filosganga/ironmq.rx.git",
      Some("scm:git:git@github.com:filosganga/ironmq.rx.git")
    )
  ),

  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8",
    // "-Xcheckinit" // for debugging only, see https://github.com/paulp/scala-faq/wiki/Initialization-Order
    // "-optimise"   // this option will slow your build
    "-Yclosure-elim",
    "-Yinline",
    "-Xverify",
    "-feature"
  ),
  javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),

  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.typesafeRepo("releases")
  ),

  libraryDependencies ++= Seq(
    spire.cats.all,
    shapeless,
    simulacrum,
    scalaArm,
    typesafe.config,
    slf4j.api,
    log4j.log4jToSlf4j,
    // -- Akka
    akka.actor,
    akka.slf4j,
    akka.stream,    
    akka.http,
    akka.httpJson.circe,
    circe.core,
    circe.generic,
    circe.parser,
    // -- Testing --
    scalaTest % Test,
    scalaCheck % Test,
    scalaMock.scalaTestSupport % Test,
    akka.httpTestKit % Test,
    akka.streamTestKit % Test,
    logback.classic % Test
  )
)

