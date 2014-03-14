package helios

import helios.api.HeliosApplication
import helios.api.HeliosAPI.AttitudeDeg

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object TestApp extends App {

//  for(i <- Range(0, 1000)){
//    Helios.ping(currentTimeMillis())
//    //ping(currentTimeMillis())
//    Thread.sleep(10)
//  }

//  for(i <- Range(0, 1000)) {
//    Helios.systemStatus match {
//      case Some(SystemStatus(m, a, s, seq)) => println(s"$m, $a, $s, $seq")
//      case None => println("No system status :(")
//    }
//
//    Thread.sleep(1000)
//  }

  val HeliosApp = HeliosApplication()
  val Helios = HeliosApp.Helios

  Thread.sleep(1000)
//  Helios.calibrateSensors map println
//  Helios.armMotors map println

  //def printer[T](v: T): Unit = println(s"test: $v")
  //Helios.systemStatusStream.subscribe(printer(_))

  class attRun extends Runnable {
    var p = 0
    var r = 0
    var y = 0

    var operation: Int => Int = x => x + 1

    def pitch = {
      if(p > 45) operation = _ - 1
      else if(p < -45) operation = _ + 1

      p = operation(p)
      p
    }

    def roll = {
      r = operation(r)
      r
    }

    def yaw = {
      y = operation(y)
      y
    }

    override def run(): Unit = Helios.setAttitude(AttitudeDeg(pitch, roll, yaw), 0.5f)
  }

  val r = new attRun
  HeliosApp.scheduler.schedule(0 millis, 1 nanos, r)
}
