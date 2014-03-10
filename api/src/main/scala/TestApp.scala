import helios.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import java.lang.System.currentTimeMillis

class TestApp extends HeliosApplication  {

  for(i <- Range(0, 1000)){
    Helios.ping(currentTimeMillis())
    Thread.sleep(10)
  }
}
