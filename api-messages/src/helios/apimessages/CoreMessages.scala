package helios.apimessages

trait GetterMessage
trait ValueMessage

object CoreMessages  {
  case class GetStatus() extends GetterMessage
  case class GetStatusStream() extends GetterMessage

  case class Status(status: String) extends ValueMessage
  case class StatusStream(status: String) extends ValueMessage
}
