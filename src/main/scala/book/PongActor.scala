package book

import akka.actor.{Actor, Props, Status}

object PongActor {
  def props(): Props = Props(new PongActor())

}

class PongActor extends Actor {
  override def receive: Receive = {
    case "Ping" => sender() ! "Pong"
    case _ => sender() ! Status.Failure(new Exception("unknown message"))
  }
}
