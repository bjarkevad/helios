package helios.test.core

import org.scalatest._
import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import scala.language.postfixOps
import helios.core.ClientSupervisor

class ClientSupervisorTest extends TestKit(ActorSystem("ClientSupervisorTest"))
with FlatSpecLike
with BeforeAndAfterAll
with ShouldMatchers
with ImplicitSender {

  def supervisor(probe: ActorRef): ActorRef =
    system.actorOf(ClientSupervisor.props((_: ActorRefFactory) => probe))

  def supervisor(probeFac: () => ActorRef) =
    system.actorOf(ClientSupervisor.props((_: ActorRefFactory) => probeFac()))

  "ClientSupervisor" should "terminate it's child after being terminated" in {
    val probe = TestProbe()
    watch(probe.ref)
    supervisor(probe.ref) ! PoisonPill
    assert(expectMsgClass(classOf[Terminated]) match {
      case Terminated(a) if a.equals(probe.ref) => true
      case _ => false
    })
  }


  //
  //  case object THROW
  //
  //  class ExceptionProbe(exception: Exception) extends Actor {
  //
  //    override def preStart() = {
  //      Thread.sleep(50)
  //      throw exception
  //    }
  //
  //    override def receive: Actor.Receive = {
  //      case THROW => throw exception
  //      case _ =>
  //    }
  //  }
  //
  //  it should "Escalate unknown exceptions" in {
  //    val s = supervisor(
  //      () => system.actorOf(Props(new ExceptionProbe(new ArrayIndexOutOfBoundsException())))
  //    )
  //    watch(s)
  //    expectTerminated(s, 100 millis)
  //  }
}