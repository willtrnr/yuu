import Common._
import Dependencies._

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "yuu",

    publishArtifact := false
  )
  .aggregate(core, scalaz)
  .dependsOn(core, scalaz)

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
