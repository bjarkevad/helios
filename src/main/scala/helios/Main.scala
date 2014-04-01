package helios

import akka.actor.{ActorRef, Props, ActorSystem}
import helios.core.actors.{GroundControl, ClientReceptionist}
import helios.core.actors.flightcontroller.{HeliosUART, MockSerial}
import akka.io.{UdpConnected, IO}
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import java.net.InetSocketAddress
import helios.util.gpio
import helios.util.gpio.HeliosGPIO

object Main extends App {
  implicit val system = ActorSystem("Main")

  lazy val groundControlProps: Props = {

    lazy val address: InetSocketAddress = {
      HeliosConfig.groundcontrolAddress
        .getOrElse(new InetSocketAddress("localhost", 14550))
    }

    GroundControl.props(IO(UdpConnected), address)
  }

  lazy val uartProps: Props = {

    lazy val uartManager: ActorRef = {
      HeliosConfig.serialdevice match {
        case Some("MOCK") => system.actorOf(MockSerial.props)
        case Some(_) => IO(Serial)
        case _ => system.actorOf(MockSerial.props)
      }
    }

    lazy val settings: SerialSettings = {
      SerialSettings(
        HeliosConfig.serialdevice.getOrElse("/dev/ttyUSB0"),
        HeliosConfig.serialBaudrate.getOrElse(115200),
        8,
        twoStopBits = false,
        Parity.None
      )
    }

    HeliosUART.props(uartManager, settings)
  }

  system.actorOf(ClientReceptionist.props(uartProps, groundControlProps), "receptionist")

//  HeliosGPIO.initialize
}

