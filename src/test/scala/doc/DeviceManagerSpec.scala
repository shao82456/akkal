package doc

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import iot.Device.{RecordTemperature, TemperatureRecorded}
import iot.DeviceGroup.{ReplyDeviceList, RequestDeviceList}
import iot.DeviceManager.{DeviceRegistered, ReplyGroupList, RequestGroupList, RequestTrackDevice}
import iot.{DeviceGroup, DeviceManager}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DeviceManagerSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("DeviceManagerSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "be able to register a device and group actor" in {
    val probe = TestProbe()
    val deviceManagerActor = system.actorOf(DeviceManager.props)

    deviceManagerActor.tell(RequestTrackDevice("group1", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor1 = probe.lastSender

    deviceManagerActor.tell(RequestTrackDevice("group2", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor2 = probe.lastSender

    deviceActor1 should !==(deviceActor2)

    deviceActor1.tell(RecordTemperature(1, 10), probe.ref)
    probe.expectMsg(TemperatureRecorded(1))

    deviceActor2.tell(RecordTemperature(2, 10), probe.ref)
    probe.expectMsg(TemperatureRecorded(2))

  }

  "return same actor for same groupId" in {
    val probe = TestProbe()
    val deviceManagerActor = system.actorOf(DeviceManager.props)

    deviceManagerActor.tell(RequestTrackDevice("group2", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val actor1 = probe.lastSender

    deviceManagerActor.tell(RequestTrackDevice("group2", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val actor2 = probe.lastSender

    actor1 should !==(actor2)
    deviceManagerActor.tell(RequestGroupList(1L),probe.ref)
    val reply = probe.expectMsgType[ReplyGroupList]
    println(reply.groups)

  }

//?
  "be able to list devices after one group shuts down" in {
//    val probe = TestProbe()
//    val deviceGroupActor = system.actorOf(DeviceGroup.props("group2"))
//
//    deviceGroupActor.tell(RequestTrackDevice("group2", "device1"), probe.ref)
//    probe.expectMsg(DeviceRegistered)
//    val toShutDown = probe.lastSender
//
//    deviceGroupActor.tell(RequestTrackDevice("group2", "device2"), probe.ref)
//    probe.expectMsg(DeviceRegistered)
//
//    probe.watch(toShutDown)
//    toShutDown ! PoisonPill
//    probe.expectTerminated(toShutDown)
//
//    deviceGroupActor.tell(DeviceGroup.RequestDeviceList(requestId = 1), probe.ref)
//    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 1, Set("device2")))

    //      deviceGroupActor.tell(RequestDeviceList(1L),probe.ref)
    //    val devices =probe.expectMsgType[ReplyDeviceList]
    //    println(devices.ids)
  }


}