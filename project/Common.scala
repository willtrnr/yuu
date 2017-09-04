import sbt._
import sbt.Keys._

import Dependencies._

object Common {

  val commonSettings = Seq(
    organization := "net.archwill.yuu",

    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.11.13", "2.12.3"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture",
      "-Ywarn-unused-import"
    ),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion
    ).map(_ % "test")
  )

}
