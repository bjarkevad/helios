name := "core-runtime" 

version := "0.0.0"

scalaVersion := "2.10.3"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

lazy val api = project

lazy val coreruntime = project.in(file("."))
  .dependsOn(api)

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
  "com.github.jodersky" %% "flow" % "1.1.0"
)

//Logging
libraryDependencies ++= Seq (
  "ch.qos.logback" % "logback-core" % "1.1.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.slf4j" % "slf4j-api" % "1.7.5"
)

testOptions in Test += Tests.Argument("-u", "./test-reports/")

scalacOptions ++= Seq("-feature", "-deprecation")
