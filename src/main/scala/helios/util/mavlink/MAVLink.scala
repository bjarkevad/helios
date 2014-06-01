package helios.util.mavlink

import akka.util.ByteString
import scala.util.{Failure, Success, Try}
import org.mavlink.messages.MAVLinkMessage
import org.mavlink.MAVLinkReader

object MAVLink {
  lazy val mlReader = new MAVLinkReader()

  /**
   * Convert a ByteString to a MAVLink Message
   * @param buffer the bytestring to convert
   * @return Success containing the MAVLink message if buffer contained valid MAVLink, Failure otherwise
   */
  def convertToMAVLink(buffer: ByteString): Try[MAVLinkMessage] = {
    Try(mlReader.getNextMessage(buffer.toArray, buffer.length)) match {
      case s@Success(m) if m != null => s
      case Success(m) => Failure(new Throwable("Not a MAVLink message"))
      case f@Failure(e) => f
    }
  }
}
