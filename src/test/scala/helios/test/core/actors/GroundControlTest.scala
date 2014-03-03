package helios.test.core.actors
import org.scalatest.{Ignore, FunSuiteLike, BeforeAndAfterAll, ShouldMatchers}
import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import helios.core.actors.GroundControl
import concurrent.duration._

import scala.language.postfixOps

@Ignore
class GroundControlTest extends TestKit(ActorSystem("GroundControlTest"))
  with FunSuiteLike
  with BeforeAndAfterAll
  with ShouldMatchers
  with ImplicitSender {

  override def afterAll() = {
    system.shutdown()
  }

  test("A") {
    val probe = TestProbe()
    val g = system.actorOf(Props(new GroundControl(probe.ref)))

    probe.send(g, GroundControl.Register(probe.ref))
    probe.expectMsg(15 seconds, GroundControl.UdpMsgRead(_))
  }
}
