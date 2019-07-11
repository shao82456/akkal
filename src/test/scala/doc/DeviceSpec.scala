package doc

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import iot.Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}
import iot.{Device, DeviceManager}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DeviceSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("DeviceSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group1", "device1"))
    deviceActor.tell(ReadTemperature(42), probe.ref)

    val response = probe.expectMsgType[RespondTemperature]
    response.requestId should ===(42L)
    response.value should ===(None)
  }

  "reply with latest temperature reading" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group1", "device1"))

    deviceActor.tell(RecordTemperature(1, 30), probe.ref)
    probe.expectMsg(TemperatureRecorded(1))

    deviceActor.tell(ReadTemperature(43), probe.ref)
    val response = probe.expectMsgType[RespondTemperature]
    response.requestId should ===(43L)
    response.value should ===(Some(30d))
  }

  "reply to registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group1", "device1"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("group1", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

  }

  "ignore wrong registration request" in {
    import scala.concurrent.duration._
    val probe=TestProbe()
    val deviceActor=system.actorOf(Device.props("group1","device1"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("group2","device1"),probe.ref)
    probe.expectNoMsg(500 milliseconds)
  }
}