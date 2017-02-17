lazy val commonSettings = Seq(
  organization := "com.itv",
  scalaVersion := "2.12.1",
  description := "A framework for writing and deploying Scala AWS Lambda Functions",
  publishArtifact in (Compile, packageBin) := true,
  publishArtifact in (Test, packageBin) := false,
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Compile, packageSrc) := true
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
