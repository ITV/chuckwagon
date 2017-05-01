package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import sbt.Keys._
import sbt._
import LoggingUtils._
import fansi.Color.Green
import fansi.Str
import Parsers._
import com.itv.aws.iam.Role
import com.itv.aws.lambda._
import com.itv.aws.s3.PutFile

object ChuckwagonPublishPlugin extends AutoPlugin {

  override def requires = sbtassembly.AssemblyPlugin && com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Publish
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckNames := {
        val cpc = chuckPublishConfig.value
        cpc.lambdaNameToHandlerMappings.map { nameAndHandler =>
          nameAndHandler.lambdaName
        }
      },
      chuckPublishSnapshot := {
        val developmentLambdaConfiguration = chuckPublishConfig.value
        import developmentLambdaConfiguration._
        val lambdas = resolvedLambdasForPublishing.value
        val code    = codeGeneratorTask.value

        streams.value.log.info(
          logMessage(
            (Str("About to upload '") ++ Green(
              code.toURI.toString
            ) ++ Str("' as AWS Lambda")).render
          )
        )

        val lambdaSnapshots =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambdaSnapshots(
              lambdas,
              jarStagingBucketName,
              PutFile(
                jarStagingS3KeyPrefix,
                codeGeneratorTask.value
              )
            )
            .foldMap(chuckSDKFreeCompiler.value)

        lambdaSnapshots.foreach { lambdaSnapshot =>
          streams.value.log.info(
            logMessage(
              lambdaSnapshot.lambda,
              (Str("Just Published Snapshot as '") ++ Green(
                lambdaSnapshot.arn.value.toString
              ) ++ Str("'")).render
            )
          )
        }

        lambdaSnapshots
      },
      chuckPublish := {
        val developmentLambdaConfiguration = chuckPublishConfig.value
        import developmentLambdaConfiguration._
        val lambdas = resolvedLambdasForPublishing.value
        val code    = codeGeneratorTask.value

        streams.value.log.info(
          logMessage(
            (Str("About to upload '") ++ Green(
              code.toURI.toString
            ) ++ Str("' as AWS Lambda")).render
          )
        )

        val publishedLambdas =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambdas(
              lambdas,
              jarStagingBucketName,
              PutFile(
                jarStagingS3KeyPrefix,
                code
              )
            )
            .foldMap(chuckSDKFreeCompiler.value)

        publishedLambdas.foreach { publishedLambda =>
          streams.value.log.info(
            logMessage(
              publishedLambda.lambda,
              (Str("Just Published Version '") ++ Green(
                publishedLambda.version.value.toString
              ) ++ Str(
                "' as '"
              ) ++ Green(publishedLambda.arn.value) ++ Str("'")).render
            )
          )
        }

        publishedLambdas
      },
      chuckPublishTo := {
        val toAliasName = environmentArgParser.value.parsed

        val publishedLambdas = chuckPublish.value

        publishedLambdas.foreach {
          publishedLambda =>
            val alias =
              com.itv.chuckwagon.deploy
                .aliasPublishedLambda(
                  publishedLambda,
                  toAliasName
                )
                .foldMap(chuckSDKFreeCompiler.value)

            streams.value.log.info(
              logMessage(
                publishedLambda.lambda,
                (Str("Just Published Version '") ++ Green(
                  alias.lambdaVersion.value.toString
                ) ++ Str("' to Environment '") ++ Green(alias.name.value) ++ Str(
                  "' as '"
                ) ++ Green(alias.arn.value) ++ Str("'")).render
              )
            )
        }

        ()
      }
    )

  private def resolvedLambdasForPublishing: Def.Initialize[Task[List[Lambda]]] = Def.taskDyn {
    val cpc = chuckPublishConfig.value
    import cpc._

    val maybeVpcConfig = maybeVpcConfigTask.value

    Def.task {
      lambdaNameToHandlerMappings.map { nameToHandler =>
        Lambda(
          deployment = LambdaDeploymentConfiguration(
            name = nameToHandler.lambdaName,
            roleARN = chuckRoleTask.value.arn,
            vpcConfig = maybeVpcConfig
          ),
          runtime = LambdaRuntimeConfiguration(
            handler = nameToHandler.lambdaHandler,
            timeout = timeout,
            memorySize = memorySize,
            deadLetterARN = deadLetterARN
          )
        )
      }
    }
  }

  private def maybeVpcConfigTask: Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    BaseHelpers.maybeVpcConfig(chuckPublishConfig.value.vpcConfigLookup)
  }
  private def codeGeneratorTask: Def.Initialize[Task[File]] = Def.taskDyn {
    chuckPublishConfig.value.codeFile
  }

  private def chuckRoleTask: Def.Initialize[Task[Role]] = Def.taskDyn {
    Def.task {
      com.itv.chuckwagon.deploy
        .getPredefinedOrChuckwagonRole(
          chuckPublishConfig.value.roleARN,
          LambdaRoles.roleNameFor(name.value)
        )
        .foldMap(chuckSDKFreeCompiler.value)
    }
  }
}
