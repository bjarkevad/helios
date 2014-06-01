package helios.util.nio

import java.nio.file.{StandardOpenOption, Paths}
import java.util.concurrent.{Executors, ExecutorService}
import java.nio.channels.AsynchronousFileChannel
import java.nio.ByteBuffer

import scala.util.Try
import scala.concurrent.{Future, Promise}

import collection.JavaConverters.setAsJavaSetConverter
import collection.mutable.Buffer
import java.nio.charset.StandardCharsets

object AsyncFileChannel {
  /**
   * Creates a new AsynchronousFileChannel
   * @param path The path of the file to open
   * @param openOptions The open options (readonly etc.)
   * @param executorService
   * @return Some(AsynchronousFileChannel) if the file is opened successfully, None otherwise
   */
  def apply(path: String, openOptions: scala.collection.Set[StandardOpenOption], executorService: ExecutorService): Option[AsynchronousFileChannel] = {
    Try(Paths.get(path)).
      flatMap(f => Try(AsynchronousFileChannel.open(f, setAsJavaSetConverter(openOptions).asJava, executorService))).toOption
  }

  /**
   * Creates a new AsynchronousFileChannel
   * @param path The path of the file to open
   * @param openOptions The open options (readonly etc.)
   * @return Some(AsynchronousFileChannel) if the file is opened successfully, None otherwise
   */
  def apply(path: String, openOptions: StandardOpenOption*): Option[AsynchronousFileChannel] = {
    AsyncFileChannel(path, openOptions.toSet, Executors.newScheduledThreadPool(4))
  }

  //TODO: Rethink naming of operations
  implicit class AsyncFileChannelOps(val afc: AsynchronousFileChannel) {
    /**
     * Read all lines in a AsynchronousFileChannel, Asynchronously
     * @return A Future containing the read String
     */
    def readAllAsync: Future[String] = {
      val p: Promise[String] = Promise()

      Try(afc.size.toInt).map {
        s =>
          val a = new Array[Byte](s)
          val bb: ByteBuffer = ByteBuffer.wrap(a, 0, s)
          Try(afc.read(bb, 0, "", CompletionHandler[Integer, String](
            v => p.complete(Try(new String(a))), e => p.failure(e))
          ))
      } recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }

    /**
     * Write data to a AsynchronousFileChannel, Asynchronously
     * @param data the data to write
     * @return A Future containing the amount of bytes written in an integer
     */
    def writeAsync(data: String): Future[Int] = {
      val p: Promise[Int] = Promise()

      Try(afc.write(ByteBuffer.wrap(data.getBytes), 0, "", CompletionHandler[Integer, String](
        v => p.complete(Try(v)), e => p.failure(e))
      )) recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }
    /**
     * Write data to a AsynchronousFileChannel, Asynchronously
     * @param data the data to write
     * @param attachment the attachment
     * @tparam A the type of the attachment
     * @return A Future containing the amount of bytes written in an integer and the attachment
     */
    def writeAsync[A](data: String, attachment: A): Future[(Int, A)] = {
      val p: Promise[(Int, A)] = Promise()

      Try(afc.write(ByteBuffer.wrap(data.getBytes), 0, attachment, CompletionHandler[Integer, A](
        v => p.complete(Try(v.toInt, attachment)), e => p.failure(e))
      )) recover {
        case e: Throwable => p.failure(e)
      }

      p.future
    }
  }

}

//TODO: Remove when determined how to read a file..
//object Watcher {
//
//  import java.nio.file.{WatchService, WatchKey, WatchEvent}
//
//  def register(path: String, watcher: WatchService, eventKinds: WatchEvent.Kind[_]*): Try[WatchKey] = {
//    val path = Paths.get(path)
//
//    Try(path.register(watcher, eventKinds: _*))
//  }
//}

object CompletionHandler {

  import java.nio.channels.CompletionHandler

  //TODO: Get rid of type parameter A
  //TODO: Rename type parameters to follow scheme: A, B, C, D?
  //TODO: Figure out of attachment is needed in the completion handlers?
  /**
   * Scala wrapper around  CompletionHandler
   * @param completedHandler Function to be run on succesful completion
   * @param failedHandler Function to be run on error
   * @tparam T The type of the executed function which needs the CompletionHandler
   * @tparam A Type of attachment
   * @return A completion handler with completed and failedHandler as implementation of completed and failed
   */
  def apply[T, A](completedHandler: T => Unit, failedHandler: Throwable => Unit): CompletionHandler[T, A] = {
    new CompletionHandler[T, A] {
      override def completed(result: T, attachment: A): Unit = completedHandler(result)

      override def failed(exc: Throwable, attachment: A): Unit = failedHandler(exc)
    }
  }
}

object FileOps {

  import java.nio.file.{Files, Path, attribute}
  import java.nio.file.attribute.PosixFilePermissions

  /**
   * Create a new file
   * @param path the path of the new file
   * @param attributes the POSIX permissions of the file
   * @return Success(Path) if the file could be created, Failure otherwise
   */
  def createFile(path: String, attributes: String): Try[Path] =
    Try(Files.createFile(Paths.get(path), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(attributes))))

  /**
   * Create a new file
   * @param path the path of the new file
   * @param attributes the POSIX permissions of the file
   * @return Success(Path) if the file could be created, Failure otherwise
   */
  def createFile(path: String, attributes: attribute.FileAttribute[_]*): Try[Path] =
    Try(Files.createFile(Paths.get(path), attributes: _*))

  /**
   * deletes a file if it exists
   * @param path the path of the file to delete
   * @return Success(true) if file deleted succesfully, Success(false) if file did not exist, Failure in case of an exception
   */
  def deleteIfExists(path: String): Try[Boolean] =
    Try(Files.deleteIfExists(Paths.get(path)))

  /**
   * checks if a file exists
   * @param path the path to check
   * @return True if the file exists
   */
  def exists(path: String): Boolean = {
    Try(Files.exists(Paths.get(path))).getOrElse(false)
  }

  /**
   * Writes a String to a file
   * @param path the path of the file to write to
   * @param data the String to write to path
   * @return Success if file written succesfully, Failure otherwise
   */
  def writeLines(path: String, data: String): Try[Path] = {
    Try(Files.write(Paths.get(path), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE))
  }

  /**
   * Reads lines from a file
   * @param path the path to read from
   * @return Success containing the read lines, if the file exists, Failure otherwise
   */
  def readLines(path: String): Try[Buffer[String]] = {
    import collection.JavaConversions._
    Try(Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8))
  }
}