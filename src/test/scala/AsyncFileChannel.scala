package helios.util.nio.test

import org.scalatest._
import scala.util.{Success, Failure, Try}
import java.nio.file.{StandardOpenOption, OpenOption}

import helios.util.nio.AsyncFileChannel._
import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps

import java.nio.ByteBuffer
import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps.delete

class AsyncFileChannel extends FunSuite {
  test("Async file channel") {
    val file = "/home/bjarke/testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    if(asyncFC.isFailure) assert(false)
    else asyncFC.map { fc =>
      fc.writeS(this.hashCode.toString, "Attachment")
      assert(io.Source.fromFile(file).getLines().mkString("") === this.hashCode.toString)
      assert(delete(file))
    }
//    val bb: ByteBuffer = ByteBuffer.wrap(Array[Byte](5))
//    asyncFC.map(f => f.read(bb, 0))
//    asyncFC.map(_.close())
//
//    asyncFC.map(f => f.readAll[String])
  }
}