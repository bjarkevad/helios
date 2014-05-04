package helios.test

import org.scalatest.{FlatSpec, Matchers}
import helios.types.Subscribers._
import helios.types.ClientTypes._
import akka.actor.ActorRef

/* Copyright 2014 Bjarke Vad Andersen, <bjarke.vad90@gmail.com> */

class ClientTypesTest extends FlatSpec with Matchers {


  lazy val clients: Subscribers = Set(
    GroundControl(ActorRef.noSender),
    API(ActorRef.noSender),
    FlightController(ActorRef.noSender),
    GenericSerialPort(ActorRef.noSender),
    MAVLinkSerialPort(ActorRef.noSender)
  )

  "ClientTypes" should "filterTypes" in {

    clients.filterTypes[GroundControl].size should be(1)


  }
}
