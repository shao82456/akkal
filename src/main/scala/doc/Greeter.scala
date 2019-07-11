package doc

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

//#greeter-companion
//#greeter-messages
object Greeter {
  //#greeter-messages
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))

  //#greeter-messages
  final case class WhoToGreet(who: String)

  case object Greet

  case object PrintDone

}

//#greeter-messages
//#greeter-companion

//#greeter-actor
class Greeter(message: String, printerActor: ActorRef) extends Actor with ActorLogging {

  import Greeter._
  import Printer._

  var greeting = ""

  def receive = {
    case WhoToGreet(who) =>
      greeting = message + ", " + who
    case Greet =>
      //#greeter-send-message
      printerActor ! Greeting(greeting)
    //#greeter-send-message
    case PrintDone =>
      log.info("greeting printed at {}", new Date())
  }
}

//#greeter-actor
