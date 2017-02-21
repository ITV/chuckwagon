lazy val commonSettings = Seq(
  organization := "com.itv",
  scalaVersion := "2.12.1",
  description := "A framework for writing and deploying Scala AWS Lambda Functions",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  )
)

val awsSdkVersion = "1.11.86"

lazy val root = (project in file("."))
  .settings(
    Seq(publishArtifact := false)
  )
  .aggregate(
    `aws-scala-sdk`,
    `sbt-chuckwagon`
  )

lazy val `aws-scala-sdk` = project
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    commonSettings ++
      Seq(
        scalaVersion := "2.10.6",
        crossScalaVersions := Seq("2.12.1", "2.10.6"),
        libraryDependencies ++= Seq(
          "com.amazonaws"  % "aws-java-sdk-iam"    % awsSdkVersion,
          "com.amazonaws"  % "aws-java-sdk-lambda" % awsSdkVersion,
          "com.amazonaws"  % "aws-java-sdk-s3"     % awsSdkVersion,
          "org.typelevel" %% "cats-free" % "0.9.0"
        )
      )
  )

lazy val `sbt-chuckwagon` = project
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
  commonSettings ++
    Seq(
      sbtPlugin := true,
      scalaVersion := "2.10.6",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "fansi" % "0.2.3"
      ),
      addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")
    )
  )
  .dependsOn(`aws-scala-sdk`)


publishTo := Some("Artifactory Realm" at "https://itvrepos.artifactoryonline.com/itvrepos/cps-libs")

releaseCrossBuild := false
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)