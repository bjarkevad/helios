package helios.core.actors.flightcontroller

import akka.actor.{Props, Actor}
import org.slf4j.LoggerFactory
import com.github.jodersky.flow.Serial
import org.mavlink.messages.common.msg_heartbeat
import org.mavlink.messages._
import helios.core.actors.ClientReceptionist.PublishMAVLink


object MockSerial {
  def props = Props(new MockSerial)
}


//TODO: Does not respond to ack requests
class MockSerial extends Actor {
  import scala.concurrent.duration._
  import scala.language.postfixOps
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val logger = LoggerFactory.getLogger(classOf[MockSerial])

  var s = 0
  def heartbeat: MAVLinkMessage = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = s
    s += 1
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }


  override def preStart() = {
    context.system.scheduler.schedule(0 millis, 1 second, context.parent, PublishMAVLink(heartbeat))
  }

  override def receive: Actor.Receive = {
    case Serial.Open(s) => sender ! Serial.Opened(s, self)

    case m@_ => logger.debug(s"Received $m")
  }
}
