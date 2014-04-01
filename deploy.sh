#!/bin/bash

HOST=192.168.7.2
DEBUG_PORT=4455
JMX_PORT=4456
JARNAME=helios.jar
DIR=./helios

sbt assembly;
scp ./target/scala-2.10/*.jar ubuntu@$HOST:~/helios/$JARNAME;
