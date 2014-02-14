package helios

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers, FlatSpec}

import com.github.jodersky.flow._
import akka.io.IO
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import helios.core.actors.SerialPort
import akka.util.ByteString
import helios.core.actors.SerialPort.WriteStuff
import concurrent.duration._

class SerialPortTest extends TestKit(ActorSystem("SerialPort"))
  with FunSuiteLike
  with BeforeAndAfterAll
  with Matchers
  with ImplicitSender {

  test("Open") {
    val port = "/dev/ttyUSB0"
    val baud = 115200
    val cs = 8
    val tsb = false
    val parity = Parity(0)

    val settings = SerialSettings(port, baud, cs, tsb, parity)

    val probe = TestProbe()

    internal.InternalSerial.debug(true)
    val sp = system.actorOf(Props(new SerialPort(settings)))

    probe.send(sp, WriteStuff(ByteString("Test".getBytes())))
  }

}
