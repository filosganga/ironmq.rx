package com.github.filosganga.ironmqrx

import akka.Done
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.{Notifying, BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.hashing.MurmurHash3

trait IronMqFixture extends AkkaFixture with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  _: Suite with Notifying =>

  private implicit val ec = ExecutionContext.global
  private var queues: Set[Queue.Name] = Set.empty

  lazy val ironMqClient: IronMqClient = IronMqClient(actorSystem)

  override protected def afterEach(): Unit = {
    Future.sequence(queues.map(ironMqClient.deleteQueue)).futureValue
    queues = Set.empty
    super.afterEach()
  }

  override protected def afterAll(): Unit = {
    ironMqClient.close()
    super.afterAll()
  }

  def givenQueue(name: Queue.Name): Queue = {
    val created = ironMqClient.createQueue(name).futureValue
    note(s"Queue created: ${created.name.value}", Some(created))
    queues = queues + created.name
    created
  }

  def givenQueue(): Queue = {
    givenQueue(Queue.Name(s"test-${MurmurHash3.stringHash(System.currentTimeMillis().toString)}"))
  }
}
