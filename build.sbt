lazy val commonSettings = Seq(
  organization := "com.itv.chuckwagon",
  scalaVersion := "2.12.1",
  description := "A framework for writing and deploying Scala AWS Lambda Functions",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)
lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := false,
  licenses := Seq("ITV-OSS" -> url("http://itv.com/itv-oss-licence-v1.0")),
  homepage := Some(url("http://io.itv.com/chuckwagon/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/itv/chuckwagon"),
      "scm:git:git@github.com:itv/chuckwagon.git"
    )
  ),
  pomExtra :=
    <developers>
    <developer>
      <id>caoilte</id>
      <name>Caoilte O'Connor</name>
      <url>http://caoilte.org</url>
    </developer>
  </developers>
)

val awsSdkVersion = "1.11.119"
val circeVersion  = "0.7.1"
val slf4jVersion  = "1.7.25"

lazy val root = (project in file("."))
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    name := "chuckwagon"
  )
  .settings(noPublishSettings)
  .aggregate(
    `aws-scala-sdk`,
    `jvm`,
    `sbt-chuckwagon`
  )

lazy val `aws-scala-sdk` = project
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    publishSettings ++
      commonSettings ++
      Seq(
        moduleName := "chuckwagon-aws-scala-sdk",
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

lazy val `jvm` = project
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    publishSettings ++
      commonSettings ++
      Seq(
        moduleName := "chuckwagon-jvm",
        scalaVersion := "2.12.1",
        crossScalaVersions := Seq("2.12.1"),
        libraryDependencies ++= Seq(
          "com.amazonaws" % "aws-lambda-java-core"   % "1.1.0" % Provided,
          "com.amazonaws" % "aws-lambda-java-events" % "1.3.0" % Provided,
          "com.amazonaws" % "aws-lambda-java-log4j"  % "1.0.0",
          "io.circe"      %% "circe-core"            % circeVersion,
          "io.circe"      %% "circe-generic"         % circeVersion,
          "io.circe"      %% "circe-parser"          % circeVersion,
          "org.slf4j"     % "slf4j-api"              % slf4jVersion,
          "org.slf4j"     % "slf4j-log4j12"          % slf4jVersion
        )
      )
  )

lazy val `sbt-chuckwagon` = project
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    publishSettings ++
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
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommandAndRemaining("sonatypeReleaseAll"),
  pushChanges
)

lazy val readme = scalatex
  .ScalatexReadme(
    projectId = "readme",
    wd = file(""),
    url = "https://github.com/itv/chuckwagon/tree/master",
    source = "Readme"
  )
  .enablePlugins(CrossPerProjectPlugin)
  .settings(commonSettings)
  .settings(
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq("2.12.1"),
    noPublishSettings,
    test := {
      run.in(Compile).toTask(" --validate-links").value
    },
    scalacOptions := scalacOptions.value.filter(_ != "-P:acyclic:force").filter(_ != "-Xlint")
  )
