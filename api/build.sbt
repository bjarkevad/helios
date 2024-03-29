import sbtassembly.Plugin.AssemblyKeys
import AssemblyKeys._

version := "0.0.0"

mainClass in assembly := Some("helios.TestApp")

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0",
  "com.netflix.rxjava" % "rxjava-scala" % "0.16.1"
)

val testingDeps = Seq (
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)

libraryDependencies ++= testingDeps
