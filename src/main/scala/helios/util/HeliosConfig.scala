package helios.util

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import scala.util.{Try, Success}
import helios.util.nio.FileOps
import helios.types.ClientTypes._
import com.github.jodersky.flow.{Parity, SerialSettings, Serial}
import helios.core.clients.Clients.ClientTypeProvider
import helios.util.HeliosConfig._

object HeliosConfig {

  case class SerialInfo(clientTypeProvider: ClientTypeProvider, device: String, baudrate: Int)

  case class FlightControllerInfo(clientTypeProvider: ClientTypeProvider, device: String, baudrate: Int)

  case class GroundControlInfo(clientTypeProvider: ClientTypeProvider, device: String, baudrate: Int)

  def apply(): HeliosConfig =
    new HeliosConfig("./helios.conf")

  def apply(configPath: String): HeliosConfig =
    new HeliosConfig(configPath)
}

class HeliosConfig(configPath: String) {
  lazy val config: Option[Config] = {
    val file =
      if (FileOps.exists(configPath))
        new File(configPath)
      else
        throw new Exception(s"Config $configPath did not exist")

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


  lazy val flightcontrollers: Seq[FlightControllerInfo] = {
    Seq.empty
  }

  lazy val groundcontrols: Seq[GroundControlInfo] = {
    Seq.empty
  }

  lazy val serialports: Seq[SerialInfo] = {
    import collection.JavaConverters._
    config.map {
      c => c.getStringList("serial").asScala.map {
        v => {
          serialDetails(v) match {
            case (ctp, dev, baud) =>
              SerialInfo(ctp,
                dev,
                baud
              )
          }
        }
      }
    }.getOrElse(List.empty)
  }


  def serialDetails(line: String): (ClientTypeProvider, String, Int) = {
    val split = line.split(' ').toList

    val device: String = split.collectFirst {
      case v if FileOps.exists(v) => v
    }.getOrElse("/dev/null")

    val baudrate: Int = Try(split.collectFirst {
      case x => x.toInt
    }).toOption.flatten.getOrElse(9600)

    val deviceType = split.collectFirst {
      case s if s != device && s != baudrate.toString => s
    } match {
      case Some("mavlink") => MAVLinkSerialPort
      case _ => GenericSerialPort
    }

    (deviceType, device, baudrate)
  }

  //  lazy val allClients: Seq[(Props, String)] = {
  //    //    addNames(generics, "Generic") ++
  //    //      addNames(flightcontrollers, "FlightController") ++
  //    //      addNames(groundcontrols, "GroundControl") ++
  //    //      addNames(serialports, "SerialPort")
  //    ???
  //  }

  //  def addNames(props: Seq[Props], basename: String): Seq[(Props, String)] =
  //    props.zipWithIndex.map(pn => (pn._1, basename + "-" + pn._2))

}
