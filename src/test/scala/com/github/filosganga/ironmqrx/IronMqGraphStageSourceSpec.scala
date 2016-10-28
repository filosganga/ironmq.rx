package com.github.filosganga.ironmqrx

import akka.stream.scaladsl.{Sink, Source}
import scala.concurrent.ExecutionContext.Implicits.global

class IronMqGraphStageSourceSpec extends UnitSpec with IronMqFixture with AkkaStreamFixture {

  "IronMqGraphStageSource" when {
    "there are messages" should {
      "consume all messages" in {
        val queue = givenQueue()
        val messages = (1 to 100).map(i => PushMessage(s"test-$i"))
        ironMqClient.pushMessages(queue.name, messages:_*).futureValue

        val source = Source.fromGraph(new IronMqGraphStageSource(queue.name, () => IronMqClient(actorSystem)))
        val receivedMessages = source.take(100).runWith(Sink.seq).map(_.map(_.body)).futureValue
        val expectedMessages = messages.map(_.body)

        receivedMessages should contain theSameElementsInOrderAs expectedMessages
      }
    }
  }

}
