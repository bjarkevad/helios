import AssemblyKeys._

name := "coreruntime"

version := "0.0.0"

scalaVersion := "2.10.4"

lazy val api = (project in file("api")).
  settings(_root_.sbtassembly.Plugin.buildSettings: _*).
  settings(assemblySettings: _*)

lazy val coreruntime = (project in file(".")).
  settings(_root_.sbtassembly.Plugin.buildSettings: _*).
  settings(assemblySettings: _*).
  dependsOn(api)

//scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value+"/root-doc.txt")

scalacOptions in (Compile, doc) ++= Seq("-doc-title", "Helios Core")

test in assembly := {}

//mainClass in assembly := Some("helios.Main")
mainClass in assembly := Some("helios.TestApp")

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "application.conf" => MergeStrategy.first
    case x => old(x)
  }
}

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "net.java.dev.jna" % "jna" % "4.0.0",
  "com.github.jodersky" %% "flow" % "1.2.0",
  "com.netflix.rxjava" % "rxjava-scala" % "0.16.1"
)

val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0"
)

libraryDependencies ++= akkaDeps

val loggingDeps = Seq(
  "ch.qos.logback" % "logback-core" % "1.1.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.slf4j" % "slf4j-api" % "1.7.5"
)

libraryDependencies ++= loggingDeps

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0" % "test"
)

libraryDependencies ++= testDeps

testOptions in Test += Tests.Argument("-u", "./test-reports/")

scalacOptions ++= Seq("-feature", "-deprecation")
