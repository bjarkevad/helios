package com.helios.core

import akka.actor.Actor
import akka.actor.Props

object Greeter {
  case object Greet
  case object Done
}

class Main extends Actor {
  override def preStart(): Unit = {
    val greeter = context.actorOf(Props[Greeter], "greeter")

    greeter ! Greeter.Greet
  }

  def receive = {
    case Greeter.Done => context.stop(self)
  }
}

class Greeter extends Actor {
  def receive = {
    case Greeter.Greet =>
      println("Hello, World!")
      sender ! Greeter.Done
  }
}
