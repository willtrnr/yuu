import sbt._
import sbt.Keys._

import Dependencies._

object Common {

  val commonSettings = Seq(
    organization := "net.archwill.yuu",

    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.11.13", "2.12.3"),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion
    ).map(_ % "test")
  )

}
