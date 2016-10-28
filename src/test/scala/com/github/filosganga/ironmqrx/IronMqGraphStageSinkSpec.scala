package com.github.filosganga.ironmqrx

import akka.stream.scaladsl._

class IronMqGraphStageSinkSpec extends UnitSpec with IronMqFixture with AkkaStreamFixture {

  "IronMqGraphStageSink" should {
    "push messages to the queue" in {

      val queue = givenQueue()
      val sink = Sink.fromGraph(new IronMqPushMessageStage(queue.name, () => IronMqClient(actorSystem)))

      val expectedMessagesBodys = List("test-1", "test-2")

      Source(expectedMessagesBodys).map(PushMessage(_)).alsoToMat(Sink.seq)(Keep.right).to(sink).run().futureValue
      val consumedMessages = ironMqClient.pullMessages(queue.name, 20).futureValue.map(_.body).toSeq

      consumedMessages should contain theSameElementsAs expectedMessagesBodys
    }
  }

}
