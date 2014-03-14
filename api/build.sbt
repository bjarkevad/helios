import AssemblyKeys._

version := "0.0.0"

mainClass in assembly := Some("helios.TestApp")

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0",
  "com.netflix.rxjava" % "rxjava-scala" % "0.16.1"
)
