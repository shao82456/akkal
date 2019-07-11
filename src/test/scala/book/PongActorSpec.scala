package book

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class PongActorSpec extends FunSpecLike with Matchers {
  val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)
  val pongActor = system.actorOf(PongActor.props())

  describe("Pong Actor") {
    it("should respond with Pong") {
      import scala.concurrent.ExecutionContext.Implicits.global
      val future = pongActor ? "Ping"
      future.onSuccess({
        case x:String=>{
          println("replied with:"+x)
        }
      })
      val result = Await.result(future.mapTo[String], 1 second)
      assert(result == "Pong")
    }
    it("should fail on unknown message") {
      val future = pongActor ? "hh"
      intercept[Exception] {
        val result = Await.result(future.mapTo[String], 1 second)
      }
    }
  }
}
