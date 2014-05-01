package helios

import akka.actor.{ActorRef, Props, ActorSystem}
import helios.core.clients.{GenericUART, MockSerial}
import akka.io.{UdpConnected, IO}
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import java.net.InetSocketAddress
import helios.util.HeliosConfig
import HeliosConfig.{MAVLink, Generic}
import helios.core.clients.MAVLinkUART
import helios.core.clients.GroundControlUDP
import helios.core.ClientReceptionist

object Main extends App {
  def HeliosInit(implicit system: ActorSystem): ActorRef = {
//
//    lazy val groundControlProps: Props = {
//
//      lazy val address: InetSocketAddress = {
//        HeliosConfig.groundcontrolAddress
//          .getOrElse(new InetSocketAddress("localhost", 14550))
//      }
//
//      GroundControlUDP.props(IO(UdpConnected), address)
//    }
//
//    lazy val uartProps: Props = {
//
//      lazy val uartManager: ActorRef = {
//        HeliosConfig.serialdevice match {
//          case Some("MOCK") => system.actorOf(MockSerial.props)
//          case Some(_) => IO(Serial)
//          case _ => system.actorOf(MockSerial.props)
//        }
//      }
//
//      lazy val settings: SerialSettings = {
//        SerialSettings(
//          HeliosConfig.serialdevice.getOrElse("/dev/ttyO2"),
//          HeliosConfig.serialBaudrate.getOrElse(115200),
//          8,
//          twoStopBits = false,
//          Parity.None
//        )
//      }
//
//      MAVLinkUART.props(uartManager, settings)
//    }
//
//    lazy val muxUartProps: Props = {
//      lazy val uartManager: ActorRef = IO(Serial)
//
//      lazy val settings: SerialSettings = {
//        SerialSettings(
//          HeliosConfig.muxSerialDevice.getOrElse("/dev/ttyO1"),
//          HeliosConfig.muxSerialBaudrate.getOrElse(115200),
//          8,
//          twoStopBits = false,
//          Parity.None
//        )
//      }
//      HeliosConfig.muxSerialType.getOrElse(Generic()) match {
//        case MAVLink() =>
//          MAVLinkUART.props(uartManager, settings)
//        case Generic() =>
//          GenericUART.props(uartManager, settings)
//      }
//    }
//    system.actorOf(
//      ClientReceptionist.props(uartProps, groundControlProps, muxUartProps), "receptionist")
    ???
  }

  implicit val system = ActorSystem("Main")
  HeliosInit
}



