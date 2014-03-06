package helios.test

import org.scalatest._

import com.github.jodersky.flow._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Terminated, PoisonPill, ActorSystem}

import helios.core.actors.flightcontroller.FlightControllerMessages.{WriteAck, WriteData}
import com.github.jodersky.flow.SerialSettings
import akka.io.IO
import scala.concurrent.duration._
import scala.language.postfixOps

import helios.core.actors.flightcontroller.HeliosUART
import akka.util.ByteString
import com.github.jodersky.flow.Serial.Received

class HeliosUARTTest extends TestKit(ActorSystem("SerialPort"))
with FlatSpecLike
with BeforeAndAfterAll
with Matchers
with ImplicitSender {

  val settings = {
    val port = "/dev/ttyUSB0"
    val baud = 115200
    val cs = 8
    val tsb = false
    val parity = Parity(0)

    SerialSettings(port, baud, cs, tsb, parity)
  }

  "HeliosUART" should "Open and register" in {
    val operator = TestProbe()
    val uart = TestProbe()

    //internal.InternalSerial.debug(true)
    val sp = system.actorOf(HeliosUART(uart.ref, settings))

    uart.expectMsg(Serial.Open(settings))
    uart.send(sp, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(sp))
  }

  it should "write data from the UART" in {
    val operator = TestProbe()
    val uart = TestProbe()
    val probe = TestProbe()

    val sp = system.actorOf(HeliosUART(uart.ref, settings))

    probe.watch(sp)

    uart.expectMsg(Serial.Open(settings))
    uart.send(sp, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(sp))

    val data = "write this data"
    val dataBs = ByteString(data.getBytes)

    probe.send(sp, WriteData(data))
    operator.expectMsg(Serial.Write(dataBs, WriteAck(dataBs)))

  }

  it should "read data from the Serial" in {
    val operator = TestProbe()
    val uart = TestProbe()

    val sp = system.actorOf(HeliosUART(uart.ref, settings))

    uart.expectMsg(Serial.Open(settings))
    uart.send(sp, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(sp))

    val data = "read this data"
    val dataBs = ByteString(data.getBytes)

    operator.send(sp, Received(dataBs))
  }

  it should "terminate with Serial" in {
    val operator = TestProbe()
    val uart = TestProbe()
    val probe = TestProbe()

    val sp = system.actorOf(HeliosUART(uart.ref, settings))

    probe.watch(sp)

    uart.expectMsg(Serial.Open(settings))
    uart.send(sp, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(sp))

    probe.send(operator.ref, PoisonPill)

    probe.expectTerminated(sp)
  }

}
