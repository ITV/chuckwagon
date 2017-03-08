package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import sbt.Keys.streams
import sbt.AutoPlugin
import LoggingUtils._
import com.itv.aws.events.ScheduleExpression
import fansi.Color.Green
import fansi.Str
import sbt.complete.DefaultParsers.{token, StringBasic}
import Parsers._
import com.itv.aws.lambda._
import com.itv.aws.s3.{BucketName, PutFile, S3KeyPrefix}

import scala.concurrent.duration._

object ChuckwagonDevelopmentPlugin extends AutoPlugin {

  override def requires = sbtassembly.AssemblyPlugin && com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Development
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckEnvironments := chuckDefineEnvironments("blue-qa", "qa"),
      chuckRuntimeConfiguration := {
        val handler          = chuckHandler.value
        val timeoutInSeconds = chuckTimeoutInSeconds.value
        val memorySizeInMB   = chuckMemorySizeInMB.value

        require(
          timeoutInSeconds > 0 && timeoutInSeconds <= 300,
          "Lambda timeout must be between 1 and 300 seconds"
        )

        require(
          memorySizeInMB >= 128 && memorySizeInMB <= 1536,
          "Lambda memory must be between 128 and 1536 MBs"
        )

        LambdaRuntimeConfiguration(
          handler = LambdaHandler(handler),
          timeout = timeoutInSeconds.seconds,
          memorySize = MemorySize(memorySizeInMB)
        )
      },
      chuckPublish := {
        val lambda = Lambda(
          deployment = chuckDeploymentConfiguration.value,
          runtime = chuckRuntimeConfiguration.value
        )

        val alias =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambdaToAlias(
              lambda,
              BucketName(chuckJarStagingBucketName.value),
              PutFile(
                S3KeyPrefix(chuckJarStagingBucketKeyPrefix.value),
                sbtassembly.AssemblyKeys.assembly.value
              ),
              chuckEnvironments.value.head.aliasName
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logMessage(
            (Str("Just Published Version '") ++ Green(
              alias.lambdaVersion.value.toString
            ) ++ Str("' to Environment '") ++ Green(alias.name.value) ++ Str(
              "' as '"
            ) ++ Green(alias.arn.value) ++ Str("'")).render
          )
        )

        ()
      }
    )
}
