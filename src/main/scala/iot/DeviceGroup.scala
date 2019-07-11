package iot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import iot.DeviceManager.{RequestGroupTemperature, RequestTrackDevice, RespondGroupTemperatureQuery}

import scala.concurrent.duration._

object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)

  final case class ReplyDeviceList(requestId: Long, ids: Set[String])


  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading

  final case class Temperature(value: Double) extends TemperatureReading

  case object TemperatureNotAvailable extends TemperatureReading

  case object DeviceNotAvailable extends TemperatureReading

  case object DeviceTimedOut extends TemperatureReading

}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIdToActor = Map.empty[String, ActorRef]
  var actorToDeviceId = Map.empty[ActorRef, String]


  import DeviceGroup._

  override def receive: Receive = {
    case trackMsg@RequestTrackDevice(`groupId`, _) =>
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(deviceActor) => deviceActor.forward(trackMsg)
        case None => {
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId))
          context.watch(deviceActor)
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          actorToDeviceId += deviceActor -> trackMsg.deviceId
          deviceActor.forward(trackMsg)
        }
      }
    case RequestTrackDevice(groupId, _) =>
      log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.", groupId, this.groupId)

    case Terminated(deviceActor) =>
      val deviceId = actorToDeviceId(deviceActor)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceId -= deviceActor
      deviceIdToActor -= deviceId

    case RequestDeviceList(id) => sender() ! ReplyDeviceList(id, deviceIdToActor.keySet)

    case RequestGroupTemperature(requestId, `groupId`) =>
      val groupQueryActor = context.actorOf(
        DeviceGroupQuery.props(actorToDeviceId, requestId, sender(), 3 seconds))
      sender() ! RespondGroupTemperatureQuery(requestId, groupQueryActor)

    case RequestGroupTemperature(requestId, groupId) =>
      log.warning("Ignoring GroupTemperature request for {} with requestId {}. This actor is responsible for {}.", groupId, requestId, this.groupId)

  }
}
