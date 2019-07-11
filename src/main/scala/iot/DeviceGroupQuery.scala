package iot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import iot.Device.{ReadTemperature, RespondTemperature}
import iot.DeviceGroup._
import iot.DeviceGroupQuery.CollectionTimeout

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {

  case object CollectionTimeout

  def props(actorToDeviceId: Map[ActorRef, String],
            requestId: Long,
            requester: ActorRef,
            timeout: FiniteDuration): Props = {
    Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))
  }
}

class DeviceGroupQuery(actorToDeviceId: Map[ActorRef, String],
                       requestId: Long,
                       requester: ActorRef,
                       timeout: FiniteDuration)
  extends Actor with ActorLogging {
  import context.dispatcher
  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach { deviceActor =>
      context.watch(deviceActor)
      deviceActor ! ReadTemperature(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  override def receive: Receive = {
    waitingForReplies(Map.empty, actorToDeviceId.keySet)
  }

  def waitingForReplies(repliesSoFar: Map[String, TemperatureReading], stillWaiting: Set[ActorRef]): Receive = {
    case RespondTemperature(0, valueOp) =>
      val deviceActor = sender()
      val reading = valueOp match {
        case Some(value) => Temperature(value)
        case None => TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { deviceActor =>
          val deviceId = actorToDeviceId(deviceActor)
          deviceId -> DeviceTimedOut
        }
      requester ! RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(
                        deviceActor: ActorRef,
                        reading: TemperatureReading,
                        stillWaiting: Set[ActorRef],
                        repliesSoFar: Map[String, TemperatureReading]): Unit = {
    context.unwatch(deviceActor)
    val deviceId=actorToDeviceId(deviceActor)
    val newStillWaiting=stillWaiting-deviceActor
    val newRepliesSoFar=repliesSoFar+(deviceId->reading)
    if(newStillWaiting.isEmpty){
      requester ! DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    }else{
      context.become(waitingForReplies(newRepliesSoFar,newStillWaiting))
    }
  }
}