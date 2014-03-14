package helios.api

/* Copyright 2014 Bjarke Vad Andersen, <bjarke.vad90@gmail.com> */

object PID extends App {

def PID(setpoint: Double) = {
  var previousError = 0.0
  var integral = 0.0
  val dt = 1

  val Kp = 0.7//0.7
  val Ki = 0.0074//1.2*Kp/100//0.05
  val Kd = 0.003//Kp*1.0001/8//0.08

  def next(setpoint: Double, measured: Double) = {
    val error = setpoint - measured
    integral = (integral + error) * dt
    val derivative = (error - previousError)/dt
    previousError = error
    val output = (Kp * error) + (Ki * integral) + (Kd * derivative)

    Thread.sleep(dt)
    output
  }

  var m = 0.0

  for(i <- Range(0, 10000)) {
    val out = next(setpoint, m)
    m = out
    println(out)
  }
}

PID(1000)
}
