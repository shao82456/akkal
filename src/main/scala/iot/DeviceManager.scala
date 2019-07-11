package iot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import iot.DeviceManager.{ReplyGroupList, RequestGroupList, RequestGroupTemperature, RequestTrackDevice, RespondGroupTemperatureQuery}

object DeviceManager {
  def props(): Props = Props(new DeviceManager)

  final case class RequestTrackDevice(groupId: String, deviceId: String)

  case object DeviceRegistered

  final case class RequestGroupList(requestId: Long)

  final case class ReplyGroupList(requestId: Long, groups: Set[String])

  final case class RequestGroupTemperature(requestId: Long, groupId: String)

  final case class RespondGroupTemperatureQuery(requestId: Long, groupQueryRef: ActorRef)

}

class DeviceManager extends Actor with ActorLogging {
  var groupIdToActor = Map.empty[String, ActorRef]
  var actorToGroupId = Map.empty[ActorRef, String]

  override def preStart(): Unit = {
    log.info("DeviceManager started")
  }

  override def postStop(): Unit = {
    log.info("DeviceManager stopped")
  }

  override def receive: Receive = {
    case trackMsg@RequestTrackDevice(groupId, _) =>
      groupIdToActor.get(groupId) match {
        case Some(deviceActor) => deviceActor.forward(trackMsg)
        case None => {
          log.info("Creating device group actor for {}", groupId)
          val groupActor = context.actorOf(DeviceGroup.props(groupId))
          context.watch(groupActor)
          groupActor.forward(trackMsg)
          groupIdToActor += groupId -> groupActor
          actorToGroupId += groupActor -> groupId
        }
      }

    case Terminated(groupActor) =>
      val groupId = actorToGroupId(groupActor)
      log.info("DeviceGroup actor for {} has been terminated", groupId)
      groupIdToActor -= groupId
      actorToGroupId -= groupActor

    case RequestGroupList(id) => sender() ! ReplyGroupList(id, groupIdToActor.keySet)

    case reqMsg@RequestGroupTemperature(requestId, groupId) =>
      groupIdToActor.get(groupId) match {
        case Some(groupActor) => groupActor.forward(reqMsg)
        case None => log.warning("DeviceGroup with groupId {}  hasn't been registered," +
            "request Temperature with {} fail", groupId,requestId)
      }
  }
}