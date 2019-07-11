import Auction.{Bid, GetBids}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.io.StdIn

object Auction{
  def props():Props=Props(new Auction())
  case class Bid(userId: String, bid: Int)
  case object GetBids

}
class Auction extends Actor with ActorLogging{
  import Auction._
  def receive = {
    case Bid=>{
      println()
    }
  }
}
object WebServer4 {


  case class Bids(bids: List[Bid])

  // these are from spray-json
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val auction = system.actorOf(Auction.props, "auction")

    val route =
      path("auction") {
        put {
          parameter("bid".as[Int], "user") { (bid, user) =>
            // place a bid, fire-and-forget
            auction ! Bid(user, bid)
            complete((StatusCodes.Accepted, "bid placed"))
          }
        }
        get {
          implicit val timeout: Timeout = 5.seconds

          // query the actor for the current auction state
          val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
          complete(bids)
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}