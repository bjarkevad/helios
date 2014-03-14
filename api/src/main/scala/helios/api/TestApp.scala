package helios.api

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global


class TestApp extends HeliosApplication  {

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

  Thread.sleep(1000)
  Helios.calibrateSensors map println
  Helios.armMotors map println

  def printer[T](v: T): Unit = println(s"test: $v")

  Helios.systemStatusStream.subscribe(printer(_))
//  Helios.terminate()
}
