lazy val commonSettings = Seq(
  organization := "com.itv",
  scalaVersion := "2.12.1",
  description := "A framework for writing and deploying Scala AWS Lambda Functions",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  ),
  publishTo := Some("Artifactory Realm".at("https://itvrepos.artifactoryonline.com/itvrepos/cps-libs"))
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

val awsSdkVersion = "1.11.93"
val circeVersion  = "0.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "chuckwagon"
  )
  .settings(noPublishSettings)
  .aggregate(
    `aws-scala-sdk`,
    `sbt-chuckwagon`,
    docs
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
          "com.amazonaws"  % "aws-java-sdk-ec2"    % awsSdkVersion,
          "com.amazonaws"  % "aws-java-sdk-events" % awsSdkVersion,
          "com.amazonaws"  % "aws-java-sdk-sts"    % awsSdkVersion,
          "io.circe"       %% "circe-core"         % circeVersion,
          "io.circe"       %% "circe-generic"      % circeVersion,
          "io.circe"       %% "circe-parser"       % circeVersion,
          "org.typelevel"  %% "cats-free"          % "0.9.0",
          "org.scala-lang" % "scala-reflect"       % scalaVersion.value // for macro paradise, for circe generic parsing
        ),
        addCompilerPlugin(("org.scalamacros" % "paradise" % "2.1.0").cross(CrossVersion.full))
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

lazy val docSettings = Seq(
  scalaVersion := "2.12.1",
  micrositeName := "Chuckwagon",
  micrositeDescription := "an AWS Lambda Deployment Toolkit for Scala/sbt",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "io.itv.com/chuckwagon",
  micrositeBaseUrl := "chuckwagon",
  micrositeGithubOwner := "itv",
  micrositeGithubRepo := "chuckwagon",
  micrositePalette := Map(
    "brand-primary"   -> "#4F1113",
    "brand-secondary" -> "#4A101B",
    "brand-tertiary"  -> "#501A11",
    "gray-dark"       -> "#49494B",
    "gray"            -> "#7B7B7E",
    "gray-light"      -> "#E5E5E6",
    "gray-lighter"    -> "#F4F3F4",
    "white-color"     -> "#FFFFFF"
  ),
//  autoAPIMappings := true,
//  ghpagesNoJekyll := false,
  fork in tut := true,
  git.remoteRepo := "git@github.com:itv/chuckwagon.git"
//  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project
  .enablePlugins(MicrositesPlugin)
  .settings(moduleName := "chuckwagon-docs")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(ghpages.settings)
  .settings(docSettings)
//  .settings(tutSettings)
  .settings(
    tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-extra-implicit")))
  )

val foo = settingKey("hoo")
