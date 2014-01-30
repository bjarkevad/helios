package helios.util.nio.test

import org.scalatest._

import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.file.{StandardOpenOption, OpenOption}
import java.nio.ByteBuffer

import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps.delete


class AsyncFileChannelTest extends FlatSpec with Matchers {
  "AsyncFileChannel" should "write, with attachment, to a file correctly" in {
    val file = "./testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    if (asyncFC.isFailure) assert(false)
    else asyncFC.get.writeS(this.hashCode.toString, "Attachment").onComplete {
      case Success((v, a)) =>
        println(s"value: $v")
        println(s"attachment: $a")
        val src = io.Source.fromFile(file)
        src.getLines().mkString("") should be(this.hashCode.toString)
        src.close()
        asyncFC.get.close()
        delete(file) should be(true)
      case Failure(e) => assert(false)
    }
  }

  it should "write, without attachment, to a file correctly" in {
    val file = "./testfile2"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    if (asyncFC.isFailure) assert(false)
    else asyncFC.get.writeS(this.hashCode.toString).onComplete {
      case Success(v) =>
        println(s"value: $v")
        val src = io.Source.fromFile(file)
        src.getLines().mkString("") should be(this.hashCode.toString)
        src.close()
        asyncFC.get.close()
        delete(file) should be(true)
      case Failure(e) => assert(false)
    }
  }

  it should "fail to write if the file path is not accessable" in {
    val file = "/testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    asyncFC.isFailure should be(true)
  }

  it should "read everything in a file correctly" in {
    val file = "./testfile3"
    val out = new java.io.FileWriter(file)
    out.write("Testfile!")
    out.close

    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.READ))
    if (asyncFC.isFailure) assert(false)
    else asyncFC.get.readAll.onComplete {
      case Success(v) =>
        v should be("Testfile!")
        println(s"Read: $v")
        asyncFC.get.close()
        delete(file)
      case Failure(e) =>
        asyncFC.get.close()
        delete(file)
        println(e.getCause)
        assert(false)
    }
  }
}