name := "core-runtime" 

version := "0.0.0"

scalaVersion := "2.10.3"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

libraryDependencies ++= Seq (
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "net.java.dev.jna" % "jna" % "4.0.0",
  "com.github.jodersky" %% "flow" % "1.1.0"
  //"javax.comm" % "comm" % "3.0-u1"
)

testOptions in Test += Tests.Argument("-u", "./test-reports/")
