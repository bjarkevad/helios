package helios.util

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import scala.util.Try
import helios.util.nio.FileOps
import helios.types.ClientTypes._
import helios.core.clients.Clients.ClientTypeProvider
import helios.util.HeliosConfig._
import org.slf4j.LoggerFactory

/**
 * HeliosConfig companion object
 */
object HeliosConfig {

  /**
   * Represents information about a serial port
   * @param clientTypeProvider factory that creates the client type
   * @param device which serial device should be opnened, e.g. "/dev/ttyO1"
   * @param baudrate the baudrate of the serial device
   */
  case class SerialInfo(clientTypeProvider: ClientTypeProvider, device: String, baudrate: Int)

  /**
   * Represents information about a Flight Controller
   * @param clientTypeProvider factory that creates the client type
   * @param device which serial device should be opened, e.g. "/dev/ttyO1"
   * @param baudrate the baudrate of the serial device
   */
  case class FlightControllerInfo(clientTypeProvider: ClientTypeProvider, device: String, baudrate: Int)

  /**
   * Represents information about a Ground Control
   * @param clientTypeProvider factory that creates the client type
   * @param address the IP address of the ground control
   * @param port the port on which the ground control listens for incoming connections
   */
  case class GroundControlInfo(clientTypeProvider: ClientTypeProvider, address: String, port: Int)

  /**
   * Creates a new instance of the HeliosConfig class, using the default location of the config file ("./helios.conf")
   * @return returns the newly created instance of HeliosConfig
   */
  def apply(): HeliosConfig =
    new HeliosConfig("./helios.conf")

  /**
   * Creates a new instance of the HeliosConfig class
   * @param configPath the path of the config file, e.g. "/path/to/helios.conf"
   * @return returns the newly created instance of HeliosConfig
   */
  def apply(configPath: String): HeliosConfig =
    new HeliosConfig(configPath)
}

/**
 * Represents a loaded config file, and operations on it
 * @param configPath the path of the config file, e.g. "/path/to/helios.conf"
 */
class HeliosConfig(configPath: String) {
  lazy val logger = LoggerFactory.getLogger(classOf[HeliosConfig])

  /**
   * a lazy value containing the loaded config, falls back to "./helios.conf"
   */
  lazy val config: Option[Config] = {
    logger.debug(s"Looking for config file: $configPath in ${System.getProperty("user.dir")}")
    val file =
      if (FileOps.exists(configPath))
        new File(configPath)
      else
        throw new Exception(s"Config ${System.getProperty("user.dir")} $configPath did not exist")

    val any = Try(ConfigFactory.parseFileAnySyntax(file).getConfig("helios"))
    val default = Try(ConfigFactory.load("helios").getConfig("helios"))

    //TODO: dafuq is this shit
    val config = any.map {
      a => default.map(a.withFallback(_)).getOrElse(a)
    }

    //Throws if config is invalid
    config.map(_.checkValid(ConfigFactory.defaultReference(), "helios"))

    config.toOption
  }

  lazy val serialPf: PartialFunction[(ClientTypeProvider, String, Int), SerialInfo] = {
    case (ctp, dev, baud) => SerialInfo(ctp, dev, baud)
  }


  lazy val serialports: Seq[SerialInfo] = {
    clientSettings(
      config,
      "serial",
      serialDetails,
      serialPf
    )
  }

  lazy val fcPf: PartialFunction[(ClientTypeProvider, String, Int), FlightControllerInfo] = {
    case (_, dev, baud) => FlightControllerInfo(FlightController, dev, baud)
  }

  lazy val flightcontrollers: Seq[FlightControllerInfo] = {
    clientSettings(
      config,
      "flightcontroller",
      serialDetails,
      fcPf
    )
  }

  lazy val gcPf: PartialFunction[(ClientTypeProvider, String, Int), GroundControlInfo] = {
    case (ctp, addr, port) => GroundControlInfo(ctp, addr, port)
  }

  lazy val groundcontrols: Seq[GroundControlInfo] = {
    clientSettings(
      config,
      "groundcontrol",
      gcDetails,
      gcPf
    )
  }

  //TODO: merge f and pf
  /**
   * Reads settings out of config and converts them to it's respective info class
   * @param config the config to read
   * @param configTag the config tag to look for in the config
   * @param f turns the config string into it's components
   * @param pf turns the components into it's respective info class
   * @tparam A components type, usually tuple, triple, etc
   * @tparam B info class type
   * @return a sequence of info classes
   */
  def clientSettings[A, B](config: Option[Config], configTag: String, f: String => A, pf: PartialFunction[A, B]): Seq[B] = {
    import collection.JavaConverters._
    config.fold(Seq.empty: Seq[B]) {
      _.getStringList(configTag).asScala.map {
        v => pf(f(v))
      }
    }
  }

  //TODO: Add support for FC CTP
  /**
   * Parses details about serial ports from the config file
   * @param line the line in the config to parse
   * @return information about the serial port in a triple
   */
  def serialDetails(line: String): (ClientTypeProvider, String, Int) = {
    val split = line.split(' ').toList

    val device: String = split.collectFirst {
      case v if FileOps.exists(v) => v
    }.getOrElse("/dev/null")

    val baudrate: Int = split.collectFirst {
      case x if Try(x.toInt).isSuccess =>
        x.toInt
    }.getOrElse(9600)

    val deviceType = split.collectFirst {
      case s if s != device && s != baudrate.toString => s
    } match {
      case Some("mavlink") => MAVLinkSerialPort
      case _ => GenericSerialPort
    }

    (deviceType, device, baudrate)
  }

  //TODO: Verify address and port
  /**
   * Parses details about ground controls from the config file
   * @param line the line in the config to parse
   * @return information about the ground control in a triple
   */
  def gcDetails(line: String): (ClientTypeProvider, String, Int) = {
    val default = ("localhost", 14550)

    val split = line.split(' ').toList

    val ap = split.find(_.contains(':')).getOrElse("")
    //val tyype = split.find(!_.contains(':')).getOrElse("")

    val addr: (String, Int) = {
      ap.split(':').toList match {
        case (a :: p :: xs) =>
          (a, p.toInt)
        case _ =>
          default
      }
    }

    (GroundControl, addr._1, addr._2)
  }
}
