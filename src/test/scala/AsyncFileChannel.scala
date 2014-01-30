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
  "AsyncFileChannel" should "write to a file correctly" in {
    val file = "./testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    if(asyncFC.isFailure) assert(false)
    else asyncFC.map { fc =>
      fc.writeS(this.hashCode.toString, "Attachment")
        .onComplete {
          case Success(v) =>
            val src = io.Source.fromFile(file)
            src.getLines().mkString("") should be(this.hashCode.toString)
            src.close()
            fc.close()
            delete(file) should be(true)
          case Failure(e) => assert(false)
        }
    }
  }
  it should "fail if the file path is not accessable" in {
    val file = "/testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    asyncFC.isFailure should be(true)
  }
}