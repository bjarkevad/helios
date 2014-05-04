package helios

import akka.actor.{Props, ActorRef, ActorSystem}
import helios.util.HeliosConfig._
import helios.util.HeliosConfig
import helios.core.ClientReceptionist

object Main extends App {
  def HeliosInit(implicit system: ActorSystem = ActorSystem("Main")): ActorRef = {
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
    def fcProps(fcinfo: Seq[FlightControllerInfo]): Seq[(Props, String)] = ???
    def serialProps(serialInfo: Seq[SerialInfo]): Seq[(Props, String)] = ???
    def gcProps(groundControlInfo: Seq[GroundControlInfo]): Seq[(Props, String)] = ???


    val config = HeliosConfig()

    val props = fcProps(config.flightcontrollers) ++
      serialProps(config.serialports) ++
      gcProps(config.groundcontrols)

    system.actorOf(ClientReceptionist.props(props))
  }

  HeliosInit
}



