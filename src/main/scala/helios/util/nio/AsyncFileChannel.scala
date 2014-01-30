package helios.util.nio

import java.nio.file.{StandardOpenOption, Paths, OpenOption}
import java.util.concurrent.{Executors, ExecutorService}
import java.nio.channels.AsynchronousFileChannel
import java.nio.ByteBuffer
import collection.JavaConverters.setAsJavaSetConverter

import scala.util.Try
import scala.concurrent.{Future, Promise}

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

  implicit class AsyncFileChannelOps(val afc: AsynchronousFileChannel) {
    def readAll[T]: T = {
      ???
      val a = Array.emptyByteArray
      def bb: ByteBuffer = ByteBuffer.wrap(a, 0, afc.size().toInt)
      afc.read(bb, 0).asInstanceOf[T]
    }

    def writeS[A](data: String, attachment: A): Future[Int] = {
      val p: Promise[Int] = Promise()
      afc.write(ByteBuffer.wrap(data.getBytes), 0, attachment, CompletionHandler[Integer, A](
        v => p.complete(Try(v)), e => p.failure(e))
      )
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
      override def failed(exc: Throwable, attachment: A): Unit = failedHandler(exc)
    }
  }
}
