import sbt._

object Plugins extends Build {
  lazy val plugins = Project("plugins", file("."))
    .dependsOn(
      uri("git://github.com/caojia/sbt-simple-junit-xml-reporter-plugin.git")
    )
} 
