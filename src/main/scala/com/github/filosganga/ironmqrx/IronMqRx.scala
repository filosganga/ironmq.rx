package com.github.filosganga.ironmqrx

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest, Uri}
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, OAuth2BearerToken, Authorization}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.Config
import org.reactivestreams.{Subscriber, Publisher}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.client.RequestBuilding._
import Codec._
import de.heikoseeberger.akkahttpcirce.CirceSupport._

import scala.util.{Failure, Success}



