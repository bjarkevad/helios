package helios.util

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import java.net.InetSocketAddress
import scala.util.Try

object HeliosConfig {
  trait SerialType
  case class MAVLink() extends SerialType
  case class Generic() extends SerialType

  lazy val config: Option[Config] = {
    val file = new File("./helios.conf")
    val any = Try(ConfigFactory.parseFileAnySyntax(file))
    val default = Try(ConfigFactory.load("helios"))

    //TODO: dafuq is this shit
    val config = any.map {
      a => default.map(a.withFallback(_)).getOrElse(a)
    }

    //Throws if config does not exist
    config.failed.map(throw new Exception("Config does not exist, exiting.."))
    //Throws if config is invalid
    config.map(_.checkValid(ConfigFactory.defaultReference(), "helios"))

    config.toOption
  }

  lazy val serialdevice: Option[String] = config.flatMap {
    c => Try(c.getString("helios.serial.device")).toOption
  }

  lazy val serialBaudrate: Option[Int] = config.flatMap {
    c => Try(c.getInt("helios.serial.baudrate")).toOption
  }

  lazy val muxSerialDevice: Option[String] = config.flatMap {
    c => Try(c.getString("helios.muxserial.device")).toOption
  }
  lazy val muxSerialBaudrate: Option[Int] = config.flatMap {
    c => Try(c.getInt("helios.muxserial.baudrate")).toOption
  }

  lazy val muxSerialType: Option[SerialType] = config.flatMap {
    c => Try(
      c.getString("helios.muxserial.type").toLowerCase match {
        case "mavlink" => MAVLink()
        case "generic" => Generic()
      }
    ).toOption
  }

  lazy val groundcontrolAddress: Option[InetSocketAddress] = {
    val host: Option[String] = config.flatMap {
      c =>
        Try(c.getString("helios.groundcontrol.host"))
          //.orElse(Try("localhost"))
          .toOption
    }

   val port: Option[Int] = config.flatMap {
      c =>
        Try(c.getInt("helios.groundcontrol.port"))
          //.orElse(Try(14550))
          .toOption
    }

    Try(new InetSocketAddress(host.get, port.get)).toOption
  }
}
