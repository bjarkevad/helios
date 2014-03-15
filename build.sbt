import sbtassembly.Plugin._
import AssemblyKeys._

name := "core-runtime"

version := "0.0.0"

scalaVersion := "2.10.3"

lazy val api = (project in file("api")).
  settings(_root_.sbtassembly.Plugin.buildSettings: _*).
  settings(assemblySettings: _*)

lazy val coreruntime = (project in file(".")).
  settings(_root_.sbtassembly.Plugin.buildSettings: _*).
  settings(assemblySettings: _*).
  dependsOn(api)

//test in assembly := {}

mainClass in assembly := Some("helios.Main")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => 
  {
    case "application.conf" => MergeStrategy.first
    case x => old(x)
  }
}

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

//Core
libraryDependencies ++= Seq (
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0"
)

//Misc
libraryDependencies ++= Seq (
  "net.java.dev.jna" % "jna" % "4.0.0",
  "com.github.jodersky" %% "flow" % "1.1.0",
  "com.netflix.rxjava" % "rxjava-scala" % "0.16.1"
)

//Logging
libraryDependencies ++= Seq (
  "ch.qos.logback" % "logback-core" % "1.1.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.slf4j" % "slf4j-api" % "1.7.5"
)

testOptions in Test += Tests.Argument("-u", "./test-reports/")

scalacOptions ++= Seq("-feature", "-deprecation")
