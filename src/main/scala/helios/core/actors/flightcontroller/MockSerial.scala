package helios.core.actors.flightcontroller

import akka.actor.{Props, Actor}
import org.slf4j.LoggerFactory
import com.github.jodersky.flow.Serial


object MockSerial {
  def props = Props(new MockSerial)
}


//TODO: Does not respond to ack requests
class MockSerial extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[MockSerial])

  override def receive: Actor.Receive = {
    case Serial.Open(s) => sender ! Serial.Opened(s, self)

    case m@_ => logger.debug(s"Received $m")
  }
}
