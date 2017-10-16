import sbt._
import sbt.Keys._

import Dependencies._

object Common {

  val commonSettings = Seq(
    organization := "net.archwill.yuu",

    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.12.3", "2.11.11"),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion
    ).map(_ % "test"),

    javacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-source", "1.8",
      "-target", "1.8",
      "-Xlint"
    ),

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-target:jvm-1.8",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused-import"
    ),

    scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
    scalacOptions in (Test, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),

    logBuffered in Test := false,

    autoAPIMappings := true
  )

}
