package book

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.util.Timeout
import AkkademyDb._
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

class AkkademyDbSpec extends FunSpecLike with Matchers{
  implicit val system=ActorSystem()
  implicit val timeout=Timeout(5 seconds)

  describe("akkademyDb"){
    describe("given SetReq"){
      it("should place key/value to map"){
        val actorRef=TestActorRef(new AkkademyDb)
        actorRef ! SetRequest("key","value")

        val akkademyDb=actorRef.underlyingActor
        akkademyDb.map.get("key") should equal(Some("value"))
      }
    }
  }

}
