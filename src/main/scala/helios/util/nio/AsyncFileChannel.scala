package helios.util.nio

import java.nio.file.{StandardOpenOption, Paths}
import java.util.concurrent.{Executors, ExecutorService}
import java.nio.channels.AsynchronousFileChannel
import java.nio.ByteBuffer

import scala.util.Try
import scala.concurrent.{Future, Promise}

import collection.JavaConverters.setAsJavaSetConverter

object AsyncFileChannel {
  def apply(path: String, openOptions: scala.collection.Set[StandardOpenOption], executorService: ExecutorService): Try[AsynchronousFileChannel] = {
    Try(Paths.get(path)).
      flatMap(f => Try(AsynchronousFileChannel.open(f, setAsJavaSetConverter(openOptions).asJava, executorService)))
  }

  def apply(path: String, openOptions: scala.collection.Set[StandardOpenOption]): Try[AsynchronousFileChannel] = {
    AsyncFileChannel(path, openOptions, Executors.newScheduledThreadPool(4))
  }

  def apply(path: String, openOption: StandardOpenOption): Try[AsynchronousFileChannel] = {
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
  def delete(path: String): Boolean = java.nio.file.Files.deleteIfExists(Paths.get(path))
}

object CompletionHandler {

  import java.nio.channels.CompletionHandler

  def apply[T, A](completedHandler: T => Unit, failedHandler: Throwable => Unit): CompletionHandler[T, A] = {
    new CompletionHandler[T, A] {
      override def completed(result: T, attachment: A): Unit = completedHandler(result)

      //TODO: Figure out of attachment is needed in the failedHandler
      override def failed(exc: Throwable, attachment: A): Unit = failedHandler(exc)
    }
  }
}
