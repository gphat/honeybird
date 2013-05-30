import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "honeybird"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "wabisabi"      %% "wabisabi"               % "2.0.1"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"
    // Add your own project settings here
  )
}
