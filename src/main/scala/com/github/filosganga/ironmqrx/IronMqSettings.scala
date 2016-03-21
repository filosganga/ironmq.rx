package com.github.filosganga.ironmqrx

import akka.actor.ActorSystem
import com.typesafe.config.Config

object IronMqSettings {

  def apply(config: Config): IronMqSettings =
    new IronMqSettings(config.getConfig("ironmq-rx"))

  def apply(as: ActorSystem): IronMqSettings =
    apply(as.settings.config)

}

class IronMqSettings(config: Config) {
  val host: String = config.getString("host")
  val projectId: String = config.getString("credentials.project-id")
  val token: String = config.getString("credentials.token")
}