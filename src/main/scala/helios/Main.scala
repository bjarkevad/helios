package helios

import akka.actor.{Props, ActorRef, ActorSystem}
import helios.util.HeliosConfig._
import helios.util.HeliosConfig
import helios.core.ClientReceptionist
import helios.core.clients.{GroundControlUDP, MAVLinkUART}
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import akka.io.{UdpConnected, IO}
import java.net.InetSocketAddress

object Main extends App {
  //TODO: Wrap all clients in ClientSupervisor.props!!
  def HeliosInit(implicit system: ActorSystem = ActorSystem("Main")): ActorRef = {

    def fcProps(fcinfo: Seq[FlightControllerInfo]): Seq[(Props, String)] = {
      fcinfo.zipWithIndex.map {
        i =>
          val fci = i._1
          (MAVLinkUART.props(
            fci.clientTypeProvider,
            IO(Serial), SerialSettings(fci.device, fci.baudrate, 8, false, Parity.None)
          ), s"FlightController-${i._2}")
      }
    }

    def serialProps(serialInfo: Seq[SerialInfo]): Seq[(Props, String)] = {
      serialInfo.zipWithIndex.map {
        i =>
          val si = i._1
          //TODO: No generic UART???
          (MAVLinkUART.props(
            si.clientTypeProvider,
            IO(Serial), SerialSettings(si.device, si.baudrate, 8, false, Parity.None)
          ), s"SerialPort-${i._2}")
      }
    }

    def gcProps(groundControlInfo: Seq[GroundControlInfo]): Seq[(Props, String)] = {
      groundControlInfo.zipWithIndex.map {
        i =>
          val gci = i._1
          (GroundControlUDP.props(
            gci.clientTypeProvider,
            IO(UdpConnected),
            new InetSocketAddress(gci.address, gci.port)
          ), s"GroundControl-${i._2}")
      }
    }

    val config = HeliosConfig()

    val props = fcProps(config.flightcontrollers) ++
      serialProps(config.serialports) ++
      gcProps(config.groundcontrols)

    //system.actorOf(ClientReceptionist.props(props))
    ???
  }

  HeliosInit
}



