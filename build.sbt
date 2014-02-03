name := "core-runtime" 

version := "0.0.0"

scalaVersion := "2.10.3"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq (
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "net.java.dev.jna" % "jna" % "4.0.0",
  "org.scalaz.stream" %% "scalaz-stream" % "0.3.1"
)