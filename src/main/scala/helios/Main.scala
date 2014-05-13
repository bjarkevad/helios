package helios

import akka.actor.{ActorRefFactory, ActorRef, ActorSystem}
import helios.core.clients.{GroundControlUDP, MAVLinkUART}
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import akka.io.{UdpConnected, IO}
import java.net.InetSocketAddress
import helios.util.HeliosConfig.{GroundControlInfo, SerialInfo, FlightControllerInfo}
import helios.core.{ClientSupervisor, ClientReceptionist}

object Main extends App {
  def HeliosInit(implicit system: ActorSystem = ActorSystem("Main")): ActorRef = {

    def wrapSupervisor(client: ActorRefFactory => ActorRef): ActorRefFactory => ActorRef = {
      (x: ActorRefFactory) => x.actorOf(ClientSupervisor.props(client))
    }

    def fc(fcinfo: Seq[FlightControllerInfo]): Seq[ActorRefFactory => ActorRef] = {
      fcinfo.map { i =>
        val props = MAVLinkUART.props(
          i.clientTypeProvider,
          IO(Serial),
          SerialSettings(i.device, i.baudrate, 8, false, Parity.None)
        )

        wrapSupervisor((x: ActorRefFactory) => x.actorOf(props))
      }
    }

    def serial(serialInfo: Seq[SerialInfo]): Seq[ActorRefFactory => ActorRef] = {
      //TODO: missing generic UART?
      serialInfo.map { i =>
        val props = MAVLinkUART.props(
          i.clientTypeProvider,
          IO(Serial),
          SerialSettings(i.device, i.baudrate, 8, false, Parity.None)
        )

        wrapSupervisor((x: ActorRefFactory) => x.actorOf(props))
      }
    }

    def gc(groundControlInfo: Seq[GroundControlInfo]): Seq[ActorRefFactory => ActorRef] = {
      groundControlInfo.map { i =>
        val props = GroundControlUDP.props(
          i.clientTypeProvider,
          IO(UdpConnected),
          new InetSocketAddress(i.address, i.port)
        )

        wrapSupervisor((x: ActorRefFactory) => x.actorOf(props))
      }
    }

    import helios.util.HeliosConfig
    val config = HeliosConfig()

    val clients: Seq[ActorRefFactory => ActorRef] = fc(config.flightcontrollers) ++
      serial(config.serialports) ++
      gc(config.groundcontrols)

    system.actorOf(ClientReceptionist.props(clients))
  }

  HeliosInit
}



