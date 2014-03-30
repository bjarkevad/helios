package helios

class PID(Kp: Double, Ki: Double, Kd: Double, dt: Double) {
  var outMax = 0.0
  var outMin = 0.0

  var previousError = 0.0
  var integral = 0.0

  def nextOutput(setpoint: Double, measured: Double): Double = {
    val error = setpoint - measured

    integral = integral + (Ki * error * dt)
    if(integral > outMax){
      println("integral hit outmax")
      integral = outMax
    }
    else if(integral < outMin){
      println("integral hit outmin")
      integral = outMin
    }

    val derivative = (error - previousError) / dt
    previousError = error

    val output = (Kp * error) + integral + (Kd * derivative)
    if(output > outMax){
      println("output hit outmax")
      outMax
    }
    else if(output < outMin){
      println("output hit outmin")
      outMin
    }
    else output
  }

  def setOutputLimits(min: Double, max: Double): Unit = {
    if(min < max) {
      outMin = min
      outMax = max

      if(integral > outMax) integral = outMax
      else if(integral < outMin) integral = outMin
    }
  }
}