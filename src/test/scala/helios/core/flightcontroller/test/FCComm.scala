package helios.core.flightcontroller.test

import org.scalatest._
import java.nio.file.{Paths, StandardOpenOption, Files}

import helios.core.flightcontroller.SPI
import helios.util.nio.{FileOps, AsyncFileChannel}
import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FCCommTest extends FlatSpec with Matchers {
  "FCComm" should "read a file line by line" in {
    SPI.read("/home/bjarke/.vimrc")(s => println(s"Callback: $s")).run
  }

  "it" should "be able to write to a file while reading" in {
    val file = "/dev/urandom"

   SPI.read(file) {
      s =>
        println(s)
    }.run
  }
}