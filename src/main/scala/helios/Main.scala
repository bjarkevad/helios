package helios
import core.clients.{GroundControlUDP, MAVLinkUART}
import core.{ClientReceptionist, ClientSupervisor}
import util.HeliosConfig.{GroundControlInfo, SerialInfo, FlightControllerInfo}
import akka.actor.{ActorRefFactory, ActorRef, ActorSystem}
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import akka.io.{UdpConnected, IO}
import java.net.InetSocketAddress

object Main extends App {
  def HeliosInit(implicit system: ActorSystem = ActorSystem("Main")): ActorRef = {

    /**
     * Helper function that wraps a client in a supervisor
     * @param client the actor factory which should be wrapped
     * @return a new actor factory which creates a ClientSupervisor in addition to the client given in the first parameter
     */
    def wrapSupervisor(client: ActorRefFactory => ActorRef): ActorRefFactory => ActorRef = {
      (x: ActorRefFactory) => x.actorOf(ClientSupervisor.props(client))
    }
    /**
     * Turns a sequence of FlightControllerInfos into it's coresponding factory function
     * @param fcinfo the sequence of FlightControllerInfos that should be turned into factories
     * @return the sequence of factory functions which create the correct actors when invoked
     */
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

    /**
     * Turns a sequence of SerialInfos into it's coresponding factory function
     * @param serialInfo the sequence of SerialInfos that should be turned into factories
     * @return the sequence of factory functions which create the correct actors when invoked
     */
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

    /**
     * Turns a sequence of GroundControlInfos into it's coresponding factory function
     * @param groundControlInfo the sequence of GroundControlInfos that should be turned into factories
     * @return the sequence of factory functions which create the correct actors when invoked
     */
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

    import util.HeliosConfig
    val config = HeliosConfig()

    val clients: Seq[ActorRefFactory => ActorRef] = fc(config.flightcontrollers) ++
      serial(config.serialports) ++
      gc(config.groundcontrols)

    system.actorOf(ClientReceptionist.props(clients))
  }

  //Initialize the system!
  HeliosInit
}



