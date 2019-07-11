package doc

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import iot.Device.{ReadTemperature, RespondTemperature}
import iot.DeviceGroup.{DeviceNotAvailable, RespondAllTemperatures, Temperature, TemperatureNotAvailable}
import iot.DeviceManager.{DeviceRegistered, RequestGroupTemperature, RequestTrackDevice, RespondGroupTemperatureQuery}
import iot.{Device, DeviceGroup, DeviceGroupQuery, DeviceManager}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class IotSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("DeviceSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "return temperature value for working devices" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(
      DeviceGroupQuery.props(
        actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
        requestId = 1,
        requester = requester.ref,
        timeout = 3 seconds))

    device1.expectMsg(Device.ReadTemperature(requestId = 0))
    device2.expectMsg(Device.ReadTemperature(requestId = 0))

    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(2.0)), device2.ref)

    requester.expectMsg(
      DeviceGroup.RespondAllTemperatures(
        requestId = 1,
        temperatures = Map("device1" -> DeviceGroup.Temperature(1.0), "device2" -> DeviceGroup.Temperature(2.0))))
  }

  "return temperature value for working devices by request group" in{
    val testProbe=TestProbe()
    val groupActor=system.actorOf(DeviceGroup.props("group2"))
    groupActor.tell(RequestTrackDevice("group2","device1"),testProbe.ref)
    groupActor.tell(RequestTrackDevice("group2","device2"),testProbe.ref)

    testProbe.expectMsg(DeviceRegistered)
    testProbe.expectMsgType[DeviceRegistered.type]

    val requester=TestProbe()
    groupActor.tell(RequestGroupTemperature(1L,"group2"),requester.ref)
    requester.expectMsgType[RespondGroupTemperatureQuery]
    requester.expectMsgType[RespondAllTemperatures]
  }

  "ignore group temperature request for wrong groupId" in{
    val testProbe=TestProbe()
    val groupActor=system.actorOf(DeviceGroup.props("group2"))
    groupActor.tell(RequestTrackDevice("group2","device1"),testProbe.ref)
    groupActor.tell(RequestTrackDevice("group2","device2"),testProbe.ref)

    testProbe.expectMsg(DeviceRegistered)
    testProbe.expectMsgType[DeviceRegistered.type]

    val requester=TestProbe()
    groupActor.tell(RequestGroupTemperature(1L,"group3"),requester.ref)
    requester.expectNoMsg(3 seconds)
  }

  "return TemperatureNotAvailable for devices with no reading" in{
    val requester=TestProbe()
    val device1=TestProbe()
    val device2=TestProbe()

    val queryActor=system.actorOf(DeviceGroupQuery.props(Map(device1.ref->"device1",device2.ref->"device2"),1L,requester.ref,3 seconds))
    device1.expectMsg(ReadTemperature(0))
    device2.expectMsg(ReadTemperature(0))

    queryActor.tell(RespondTemperature(0,Some(10.0)),device1.ref)
    queryActor.tell(RespondTemperature(0,None),device2.ref)

    requester.expectMsg(RespondAllTemperatures(1L,
      Map("device2"->TemperatureNotAvailable,"device1"->Temperature(10.0))))
  }

  "return DeviceNotAvailable if device stops before answering" in{
    val requester=TestProbe()

    val device1=TestProbe()
    val device2=TestProbe()

    val queryActor=system.actorOf(DeviceGroupQuery.props(
      Map(device1.ref->"device1",device2.ref->"device2"),1L,requester.ref,3 seconds))

    device1.expectMsg(ReadTemperature(0))
    device2.expectMsg(ReadTemperature(0))

    queryActor.tell(RespondTemperature(0,Some(30.0)),device1.ref)
    device2.ref ! PoisonPill

    requester.expectMsg(RespondAllTemperatures(1L,
      Map("device1"->Temperature(30.0),"device2"->DeviceNotAvailable)))
  }

  "return temperature if device stops after answering" in{
    val requester=TestProbe()

    val device1=TestProbe()
    val device2=TestProbe()

    val queryActor=system.actorOf(DeviceGroupQuery.props(
      Map(device1.ref->"device1",device2.ref->"device2"),1L,requester.ref,3 seconds))

    device1.expectMsg(ReadTemperature(0))
    device2.expectMsg(ReadTemperature(0))

    queryActor.tell(RespondTemperature(0,Some(30.0)),device1.ref)
    queryActor.tell(RespondTemperature(0,Some(32.0)),device2.ref)
    device2.ref ! PoisonPill

    requester.expectMsg(RespondAllTemperatures(1L,
      Map("device1"->Temperature(30.0),"device2"->Temperature(32.0))))
  }

  "return DeviceTimedOut if device does not answer in time" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(
      DeviceGroupQuery.props(
        actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
        requestId = 1,
        requester = requester.ref,
        timeout = 1.second))

    device1.expectMsg(Device.ReadTemperature(requestId = 0))
    device2.expectMsg(Device.ReadTemperature(requestId = 0))

    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)
    Thread.sleep(1500)
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(-10.0)), device2.ref)

    requester.expectMsg(
      DeviceGroup.RespondAllTemperatures(
        requestId = 1,
        temperatures = Map("device1" -> DeviceGroup.Temperature(1.0), "device2" -> DeviceGroup.DeviceTimedOut)))
  }

  "be able to collect temperature from all active devices" in{
    val probe=TestProbe()
    val managerActor=system.actorOf(DeviceManager.props())

    managerActor.tell(RequestTrackDevice("group1","device1"),probe.ref)
    probe.expectMsg(DeviceRegistered)
    val device1=probe.lastSender

    managerActor.tell(RequestTrackDevice("group1","device2"),probe.ref)
    probe.expectMsg(DeviceRegistered)
    val device2=probe.lastSender

    val requester=TestProbe()

    managerActor.tell(RequestGroupTemperature(1L,"group1"),requester.ref)
    probe.expectMsgType[RespondGroupTemperatureQuery]
    val groupQueryActor=probe.lastSender

    groupQueryActor.tell(RespondTemperature(0L,Some(10.0)),device1)
    groupQueryActor.tell(RespondTemperature(0L,Some(11.0)),device2)

    requester.expectMsg(RespondAllTemperatures(1L,Map("device1"->Temperature(10.0),"device2"->Temperature(11.0))))
  }
}