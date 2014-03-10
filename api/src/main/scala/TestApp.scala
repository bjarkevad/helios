import helios.api._
import helios.api.HeliosAPI.SystemStatus
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import java.lang.System.currentTimeMillis

class TestApp extends HeliosApplication  {

//  for(i <- Range(0, 1000)){
//    Helios.ping(currentTimeMillis())
//    //ping(currentTimeMillis())
//    Thread.sleep(10)
//  }

  for(i <- Range(0, 1000)) {
    Helios.systemStatus match {
      case Some(SystemStatus(m, a, s, seq)) => println(s"$m, $a, $s, $seq")
      case None => println("No system status :(")
    }

    Thread.sleep(1000)
  }

  def ping(ms: Long) = {
    println(currentTimeMillis() - ms)
  }
}
