package helios.test.api

import akka.testkit._
import akka.actor._
import org.scalatest.{BeforeAndAfterEach, Matchers, FlatSpecLike}
import helios.api._
import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mavlink.messages.common.{msg_attitude, msg_heartbeat}
import org.mavlink.messages._
import scala.concurrent.ExecutionContext.Implicits.global
import helios.api.HeliosAPI._
import helios.messages.DataMessages.PublishMAVLink
import helios.api.HeliosAPI.SystemStatus
import helios.messages.CoreMessages.RegisterAPIClient
import helios.api.HeliosAPI.AttitudeRad
import helios.api.HeliosAPI.AttitudeDeg


abstract class APITestBase extends TestKit(ActorSystem("APITest"))
with ImplicitSender
with FlatSpecLike
with Matchers
with BeforeAndAfterEach {

  /*
+–––––––––––+    +–––––––––+      +––––––––+
| heliosApp |    | client  |      | helios |
|           +––––+ (probe) +––––––+        |
+–––––––––––+    +–––––––––+      +––––––––+
*/
  val probe: TestProbe = TestProbe()
  val client: TestProbe = TestProbe()
  val uart: TestProbe = TestProbe()
  implicit val atMost: FiniteDuration = 2 second

  lazy val heliosApp: Option[HeliosApplication] = {
    val res = Future {
      //HeliosApplication.apply blocks!
      Option(HeliosRemote(system, probe.ref.path))
    }

    probe.expectMsgClass(classOf[RegisterAPIClient])
    val heliosapi: HeliosAPI = TypedActor(system).typedActorOf(TypedProps(
      classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, client.ref, 20)))

    probe.send(probe.sender, heliosapi)
    res.block
  }

  lazy val helios: Option[HeliosAPI] = {
    heliosApp map(_.Helios)
  }

  implicit class blocker[T](a: Awaitable[T]) {
    def block(implicit atMost: Duration): T =
      Await.result(a, atMost)
  }

  implicit class actorreffor(hapi: HeliosAPI) {
    def ref: ActorRef =
      helios.map(TypedActor(system).getActorRefFor(_)).get
  }

  implicit class HeliosAppImp(happ: HeliosApplication) {
    def ref: ActorRef =
      heliosApp.map(TypedActor(system).getActorRefFor(_)).get
  }

  lazy val preflight = {
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

  lazy val flying: msg_heartbeat = {
    val hb = preflight
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb
  }

  lazy val critical: msg_heartbeat = {
    val hb = preflight
    hb.system_status = MAV_STATE.MAV_STATE_CRITICAL
    hb
  }

  lazy val emergency: msg_heartbeat = {
    val hb = preflight
    hb.system_status = MAV_STATE.MAV_STATE_EMERGENCY
    hb
  }

  def forwardToApp[T](msgType: Class[T]) {
    client.expectMsgClass(classOf[PublishMAVLink]) //Stream
    val m: T = client.expectMsgClass(msgType) //Interpreted message
    client.forward(heliosApp.get.ref, m)
  }

  def setStatus(hb: msg_heartbeat): Unit = helios.map {
    h =>
      uart.send(h.ref, PublishMAVLink(hb))
      forwardToApp(classOf[SystemStatus])
  }

  def setAttitude(att: Attitude): Unit = helios.map {
    h =>
      val msg = new msg_attitude(1337, 1)

      att match {
        case AttitudeDeg(r, p, y) =>
          msg.roll = Math.toRadians(r).toFloat
          msg.pitch = Math.toRadians(p).toFloat
          msg.yaw = Math.toRadians(y % 360).toFloat

        case AttitudeRad(r, p, y) =>
          msg.roll = r
          msg.pitch = p
          msg.yaw = y % (2 * Math.PI).toFloat
      }

      uart.send(h.ref, PublishMAVLink(msg))

      forwardToApp(classOf[AttitudeRad])
  }
}