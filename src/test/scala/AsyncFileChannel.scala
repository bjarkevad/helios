package helios.util.nio.test

import org.scalatest._

import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.file.{StandardOpenOption, OpenOption}
import java.nio.ByteBuffer

import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps._
import scala.concurrent.Await
import scala.concurrent.duration._


class AsyncFileChannelTest extends FlatSpec with Matchers {
  "AsyncFileChannel" should "write, with attachment, to a file correctly" in {
    val file = "./testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))

    if (asyncFC.isEmpty) assert(false)
    else {
      val f = asyncFC.get.writeS(this.hashCode.toString, "Attachment")

      Await.result(f, 200 millis) match {
        case (value, attachment) =>
          value should be(this.hashCode.toString.length)
          attachment should be("Attachment")
        case _ => assert(false)
      }

      val src = io.Source.fromFile(file)
      src.getLines().mkString("") should be(this.hashCode.toString)
      src.close()
      asyncFC.get.close()
      deleteIfExists(file).getOrElse(false) should be(true)

    }
  }

  it should "write, without attachment, to a file correctly" in {
    val file = "./testfile2"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    if (asyncFC.isEmpty) assert(false)
    else {
      val f = asyncFC.get.writeS(this.hashCode.toString)

      Await.result(f, 200 millis) should be(this.hashCode.toString.length)
      asyncFC.get.close()
      val src = io.Source.fromFile(file)
      src.getLines().mkString("") should be(this.hashCode.toString)
      src.close()
      deleteIfExists(file).getOrElse(false) should be(true)
    }
  }

  it should "fail to write if the file path is not accessible" in {
    val file = "/testfile"
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
    asyncFC.isEmpty should be(true)
  }

  it should "read everything in a file correctly" in {
    val file = "./testfile3"
    val out = new java.io.FileWriter(file)
    out.write("Testfile!")
    out.close

    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.READ))
    if (asyncFC.isEmpty) {
      assert(false)
    }
    else {
      val f = asyncFC.get.readAll
      Await.result(f, 100 millis) should be("Testfile!")
      asyncFC.get.close()
      deleteIfExists(file).getOrElse(false) should be(true)
    }
  }

  it should "fail to read if OpenOptions are set wrong" in {
    val file = "./testfile3"
    val out = new java.io.FileWriter(file)
    out.write("Testfile!")
    out.close

    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.WRITE))
    if (asyncFC.isEmpty) {
      asyncFC.isEmpty should be(false)
    }
    else {
      val f = asyncFC.get.readAll
      (Try(Await.result(f, 100 millis)) match {
        case Failure(e) => true
        case _ => false
      }) should be (true)
      asyncFC.get.close()
    }

    deleteIfExists(file).getOrElse(false) should be(true)
  }
}