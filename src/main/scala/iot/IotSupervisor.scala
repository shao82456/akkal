package iot

import akka.actor.{Actor, ActorLogging, Props}

object IotSupervisor {
  def props():Props=Props(new IotSupervisor)
}

class IotSupervisor extends Actor with ActorLogging{
  override def receive: Receive = Actor.emptyBehavior
}