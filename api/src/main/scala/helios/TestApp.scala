package helios

import helios.api.HeliosApplication
import helios.api.HeliosAPI.AttitudeDeg
import helios.api.Streams._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import java.lang.System.currentTimeMillis
import scala.concurrent.Await
import akka.util.ByteString

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

  Helios.uartStream.subscribe(println(_))

  val runner = new run
  HeliosApp.scheduler.schedule(0 seconds, 500 millis, runner)

  class run extends Runnable {
    var i = 0
    override def run(): Unit = {
      Helios.writeToUart(ByteString(i))
      i += 1
    }
  }

//  Helios.takeControl()
//  Helios.calibrateSensors map println
//  Helios.armMotors map println

  //Helios.systemStatusStream.subscribe(println(_))
//  Helios.attitudeRadStream.subscribe(v => println(s"Rad: $v"))
//  Helios.attitudeDegStream.subscribe(v => println(s"Deg: $v"))

//  class attRun extends Runnable {
//    var p = 0
//    var r = 0
//    var y = 0
//
//    var operation: Int => Int = x => x + 1
//
//    def pitch = {
//      if(p > 45) operation = _ - 1
//      else if(p < -45) operation = _ + 1
//
//      p = operation(p)
//      p
//    }
//
//    def roll = {
//      r = operation(r)
//      r
//    }
//
//    def yaw = {
//      y = operation(y)
//      y
//    }
//
//    override def run(): Unit = {
//      Helios.setAttitude(AttitudeDeg(pitch, roll, yaw), 0.5f)
//    }
//  }
//
//  val r = new attRun
//  //HeliosApp.scheduler.schedule(0 millis, 1 seconds, r)
//  HeliosApp.scheduler.schedule(0 millis, 1 nanos, r)
//  while(true) {
//    try {
//      Await.result(Helios.setAttitude(AttitudeDeg(10, 15, 20), 0.5f), 10 millis)
//      print('*')
//    }
//    catch {
//      case _: Throwable => print('-')
//    }
//  }
}
