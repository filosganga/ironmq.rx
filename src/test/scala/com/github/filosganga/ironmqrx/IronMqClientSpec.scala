package com.github.filosganga.ironmqrx

import scala.concurrent.ExecutionContext.Implicits.global

class IronMqClientSpec extends UnitSpec with AkkaFixture {


  "IronMqClient" when {
    "listQueues is called" should {
      "return the list of queue" in withIronMqClient { ironMq =>
        ironMq.listQueues().futureValue should not be empty
      }
    }
  }

  def withIronMqClient[T](f: IronMqClient => T): T = {
    resource.managed(IronMqClient(actorSystem)).acquireAndGet(f)
  }

}
