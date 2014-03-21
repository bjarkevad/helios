package helios.api.test

import org.scalatest._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorRef, TypedProps, TypedActor, ActorSystem}
import helios.api.{HeliosAPI, HeliosApplication}
import helios.core.actors.HeliosAPIDefault
import scala.concurrent.ExecutionContext.Implicits.global
import java.lang.System.currentTimeMillis
import scala.concurrent.{Awaitable, Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.api.HeliosAPI.{CommandFailure, CommandSuccess}
import org.mavlink.messages.common._
import org.mavlink.messages._
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.api.HeliosApplicationDefault.RegisterAPIClient

class APITest extends TestKit(ActorSystem("APITest"))
with ImplicitSender
with FlatSpecLike
with Matchers
with BeforeAndAfter
with BeforeAndAfterAll {

  var helios: Option[HeliosAPI] = None
  val probe: TestProbe = TestProbe()
  val client: TestProbe = TestProbe()
  val uart: TestProbe = TestProbe()
  implicit val atMost: FiniteDuration = 2 second

  implicit class blocker[T](a: Awaitable[T]) {
    def block(implicit atMost: Duration): T = {
      Await.result(a, atMost)
    }
  }

  implicit class actorreffor(hapi: HeliosAPI) {
    def ref: ActorRef =
      helios.map(TypedActor(system).getActorRefFor(_)).get
  }

  lazy val hbdefault = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

  lazy val hbflying: msg_heartbeat = {
    val hb = hbdefault
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb
  }

  def setStatus(hb: msg_heartbeat): Unit = helios.map {
    h =>
      uart.send(h.ref, PublishMAVLink(hb))
  }

  before {
    Future {
      //HeliosApplication.apply blocks!
      helios = Option(HeliosApplication(system, probe.ref.path).Helios)
    }

    probe.expectMsgClass(classOf[RegisterAPIClient])
    val heliosapi: HeliosAPI = TypedActor(system).typedActorOf(TypedProps(
      classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, client.ref, uart.ref, 20)))

    probe.send(probe.sender, heliosapi)
  }

  after {
    helios map (_.terminate())
  }

  "Helios API" should "connect correctly to the core" in helios.map {
    h =>
      val res = h.ping(currentTimeMillis).block
      println(s"Time: $res")
      (res >= 0) should be(true)
  }

  it should "ONLY be able to calibrate sensors when not flying" in helios.map {
    h =>
      setStatus(hbdefault)
      h.calibrateSensors map (_ should be(CommandSuccess()))
      uart.expectMsgClass(classOf[WriteMAVLink])

      setStatus(hbflying)
      h.calibrateSensors.block.getClass should be(classOf[CommandFailure])
  }

  it should "" in helios.map {
    h =>

  }

}