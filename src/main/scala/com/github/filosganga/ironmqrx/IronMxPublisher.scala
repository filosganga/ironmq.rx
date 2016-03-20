package com.github.filosganga.ironmqrx

import akka.actor.Actor.Receive
import akka.stream.actor.{RequestStrategy, ActorSubscriber, ActorPublisher, ActorSubscriberMessage}

class IronMxPublisher extends ActorSubscriber {
  import ActorSubscriberMessage._
  import context._

  override protected def requestStrategy: RequestStrategy = ???

  override def receive: Receive = {
    case OnNext(message) =>
    case OnError(error) =>
    case OnComplete =>
  }

}
