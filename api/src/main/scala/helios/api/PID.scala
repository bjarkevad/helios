package helios.api

/* Copyright 2014 Bjarke Vad Andersen, <bjarke.vad90@gmail.com> */

object PID extends App {

  class PID(Kp: Double, Ki: Double, Kd: Double, dt: Int) {
    var previousError = 0.0
    var integral = 0.0

    def nextOutput(setpoint: Double, measured: Double): Double = {
      val error = setpoint - measured
      integral = integral + (error * dt)
      val derivative = (error - previousError) / dt
      previousError = error

      (Kp * error) + (Ki * integral) + (Kd * derivative)
    }
  }

  val dt = 10
  val pid = new PID(1, 0.05, 0.05, dt)
  var prevOut: Double = 0

  for (i <- Range(0, 1000)) {
    prevOut = pid.nextOutput(1000, 0.20 * prevOut)
    println(prevOut)
    Thread.sleep(dt)
  }
}
