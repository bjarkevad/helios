package helios.examples

import helios.api.{HeliosLocal, HeliosAPI}
import scala.language.postfixOps
import helios.api.HeliosAPI.{CommandFailure, CommandSuccess, CommandResult}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TestApp extends App {

  val HeliosApp = HeliosLocal()
  implicit val Helios = HeliosApp.Helios

  def inputHandler(input: Char)(implicit helios: HeliosAPI): Future[CommandResult] = {
    input match {
      case 'a' => helios.armMotors
      case 'd' => helios.disarmMotors
      case 't' => helios.takeControl(); Future(CommandSuccess())
      case '6' => helios.setAttitude(HeliosAPI.AttitudeDeg(45, 0, 0), 50)
      case '4' => helios.setAttitude(HeliosAPI.AttitudeDeg(-45, 0, 0), 50)
      case '8' => helios.setAttitude(HeliosAPI.AttitudeDeg(0, 45, 0), 50)
      case '2' => helios.setAttitude(HeliosAPI.AttitudeDeg(0, -45, 0), 50)
      case '0' => helios.setAttitude(HeliosAPI.AttitudeDeg(0, 0, 0), 0)
      case '5' => helios.setAttitude(HeliosAPI.AttitudeDeg(0, 0, 0), 50)
      case c@_ => Future(CommandFailure(new Exception(s"Command not found $c")))
    }
  }

  while(true) {
    inputHandler(readChar()).map(println)
  }
}
