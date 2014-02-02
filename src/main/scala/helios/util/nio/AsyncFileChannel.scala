package helios.util.nio

import java.nio.file.{StandardOpenOption, Paths}
import java.util.concurrent.{Executors, ExecutorService}
import java.nio.channels.AsynchronousFileChannel
import java.nio.ByteBuffer

import scala.util.Try
import scala.concurrent.{Future, Promise}

import collection.JavaConverters.setAsJavaSetConverter

object AsyncFileChannel {
  def apply(path: String, openOptions: scala.collection.Set[StandardOpenOption], executorService: ExecutorService): Option[AsynchronousFileChannel] = {
    Try(Paths.get(path)).
      flatMap(f => Try(AsynchronousFileChannel.open(f, setAsJavaSetConverter(openOptions).asJava, executorService))).toOption
  }

  def apply(path: String, openOptions: scala.collection.Set[StandardOpenOption]): Option[AsynchronousFileChannel] = {
    AsyncFileChannel(path, openOptions, Executors.newScheduledThreadPool(4))
  }

  def apply(path: String, openOption: StandardOpenOption): Option[AsynchronousFileChannel] = {
    AsyncFileChannel(path, Set(openOption), Executors.newScheduledThreadPool(4))
  }

  //TODO: Rethink naming of operations
  implicit class AsyncFileChannelOps(val afc: AsynchronousFileChannel) {
    def readAll: Future[String] = {
      val p: Promise[String] = Promise()

      val a = new Array[Byte](afc.size().toInt)
      val bb: ByteBuffer = ByteBuffer.wrap(a, 0, afc.size().toInt)
      Try(afc.read(bb, 0, "", CompletionHandler[Integer, String](
        v => p.complete(Try(new String(a))), e => p.failure(e))
      )) recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }

    def writeS(data: String): Future[Int] = {
      val p: Promise[Int] = Promise()

      Try(afc.write(ByteBuffer.wrap(data.getBytes), 0, "", CompletionHandler[Integer, String](
        v => p.complete(Try(v)), e => p.failure(e))
      )) recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }

    def writeS[A](data: String, attachment: A): Future[(Int, A)] = {
      val p: Promise[(Int, A)] = Promise()

      Try(afc.write(ByteBuffer.wrap(data.getBytes), 0, attachment, CompletionHandler[Integer, A](
        v => p.complete(Try(v, attachment)), e => p.failure(e))
      )) recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }
  }

}

object FileOps {

  import java.nio.file.{Files, Path, attribute}

  def createFile(path: String, attributes: attribute.FileAttribute[_]*): Try[Path] =
    Try(Files.createFile(Paths.get(path), attributes: _*))

  def deleteIfExists(path: String): Try[Boolean] =
    Try(Files.deleteIfExists(Paths.get(path)))
}

object CompletionHandler {

  import java.nio.channels.CompletionHandler

  //TODO: Get rid of type parameter A
  //TODO: Rename type parameters to follow scheme: A, B, C, D?
  //TODO: Figure out of attachment is needed in the completion handlers?
  def apply[T, A](completedHandler: T => Unit, failedHandler: Throwable => Unit): CompletionHandler[T, A] = {
    new CompletionHandler[T, A] {
      override def completed(result: T, attachment: A): Unit = completedHandler(result)

      override def failed(exc: Throwable, attachment: A): Unit = failedHandler(exc)
    }
  }
}
