package helios.examples

//Import the needed packages
import helios.api.{HeliosRemote, HeliosLocal}
import scala.concurrent.Await
import scala.concurrent.duration._
import helios.api.HeliosAPI.AttitudeDeg

//Create an application named "Tutorial"
object Tutorial extends App {

  //Connect to a local system
  val local = HeliosLocal()

  //Connect to a remote system
  val remote = HeliosRemote("192.168.1.50", 1234)

  object functions {
    local.Helios.takeOff(1).map(result => println(s"I got the result: $result"))
    println("I'm not waiting for takeOff to finish..")

    val result = Await.result(local.Helios.takeOff(1), 2 seconds)
    println("I'm waiting for takeOff to finish..")

    val wantedAttitude = AttitudeDeg(10,5,0)
    local.Helios.setAttitude(wantedAttitude, 0.5f)

    local.Helios.systemStatus.map(status => println(s"The system status is: $status"))

    local.Helios.calibrateSensors.map(_ => println(s"Sensors calibrated!"))

    import helios.api.Handlers._
    def deployParachute(): Unit = {
      println("Deploying Parachute!")
    }

    local.Helios.setEmergencyHandler(deployParachute)

  }
}
