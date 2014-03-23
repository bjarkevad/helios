package helios

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import java.net.InetSocketAddress
import scala.util.Try

object HeliosConfig {
  lazy val config: Option[Config] = {
    val file = new File("./helios.conf")
    val any = Try(ConfigFactory.parseFileAnySyntax(file))
    val default = Try(ConfigFactory.load("helios"))

    any.map {
      a => default.map(a.withFallback(_)).getOrElse(a)
    }.toOption
  }

  lazy val serialdevice: Option[String] = config.flatMap {
    c => Try(c.getString("helios.serial.device")).toOption
  }

  lazy val serialBaudrate: Option[Int] = config.flatMap {
    c => Try(c.getInt("helios.serial.baudrate")).toOption
  }

  lazy val groundcontrolAddress: Option[InetSocketAddress] = {
    val host: Option[String] = config.flatMap {
      c => Try(c.getString("helios.groundcontrol.host")).toOption
    }

    val port: Option[Int] = config.flatMap {
      c => Try(c.getInt("helios.groundcontrol.port")).toOption
    }

    Try(new InetSocketAddress(host.get, port.get)).toOption
  }
}
