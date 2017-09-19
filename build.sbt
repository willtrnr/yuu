import Common._
import Dependencies._

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "yuu",

    publishArtifact := false
  )
  .aggregate(core, scalaz, akka)
  .dependsOn(core, scalaz, akka)

lazy val core = (project in file("./yuu-core"))
  .settings(commonSettings)
  .settings(
    name := "yuu-core",

    libraryDependencies ++= Seq(
      "org.apache.poi" % "poi" % poiVersion,
      "org.apache.poi" % "poi-ooxml" % poiVersion
    )
  )

lazy val scalaz = (project in file("./yuu-scalaz"))
  .settings(commonSettings)
  .settings(
    name := "yuu-scalaz",

    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion
    )
  )
  .aggregate(core)
  .dependsOn(core)

lazy val akka = (project in file("./yuu-akka"))
  .settings(commonSettings)
  .settings(
    name := "yuu-akka",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )
  .aggregate(core)
  .dependsOn(core)
