package helios.test.hardware

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.{Ignore, BeforeAndAfter, Matchers, FlatSpecLike}
import helios.util.nio.FileOps
import akka.io.IO
import com.github.jodersky.flow.{SerialSettings, Serial}
import akka.util.ByteString
import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat
import com.github.jodersky.flow.SerialSettings
import helios.mavlink.MAVLink.convertToMAVLink
import scala.util.Success
import helios.core.actors.uart.MAVLinkUART
import helios.core.actors.uart.DataMessages.{WriteMAVLink, WriteData}
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class serialport extends TestKit(ActorSystem("SerialPort"))
with FlatSpecLike
with Matchers
with ImplicitSender
with BeforeAndAfter {

  val port = "/dev/ttyUSB0"
  val settings = SerialSettings(port, 115200)

  val receiver = TestProbe()

  lazy val heartbeat: MAVLinkMessage = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

  def heliosUart: ActorRef = {
    val s = system.actorOf(MAVLinkUART.props(IO(Serial), settings))
    Thread.sleep(100)
    s
  }

  case class Ack() extends Serial.Event

  before {
    FileOps.exists(port) should be(true)
  }

  def open: ActorRef = {
    IO(Serial) ! Serial.Open(settings)
    val op = expectMsgClass(classOf[Serial.Opened]).operator
    op ! Serial.Register(receiver.ref)
    println("Opened")
    op
  }

  def close(operator: ActorRef) = {
    println("Closing")
    operator ! Serial.Close
    receiver.expectMsg(Serial.Closed)
  }

  "SerialPort" should "open, register and close" in {
    close(open)
  }

  it should "write then read if looped-back" in {
    val op = open
    val data = ByteString()

    for (i <- 0 until 1000) {
      op ! Serial.Write(data, Ack())
      expectMsg(Ack())
      val m = receiver.expectMsgClass(classOf[Serial.Received])
      println(s"Received: ${m.data}")
      //m.data should be(data)
    }
    //expectMsg(Serial.Received(data))

    close(op)
  }

  it should "write and read a MAVLink message if looped back" in {
    val op = open
    val data = ByteString(heartbeat.encode())


    op ! Serial.Write(data, Ack())
    expectMsg(Ack())
    val msg = receiver.expectMsgClass(classOf[Serial.Received])
    println(msg)
  }

  it should "work correctly with HeliosUART" in {
    println(ByteString(heartbeat.encode()).length)
    val uart = heliosUart

    Future {
      for (i <- 0 until 100){
        Thread.sleep(50)
        uart ! WriteMAVLink(heartbeat)
      }
    }

    for (i <- 0 until 100) {
      val m = expectMsgClass(classOf[PublishMAVLink])
      println(i)
    }
  }
}
