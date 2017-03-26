package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import sbt.Keys.streams
import sbt.AutoPlugin
import sbt.Def
import sbt.File
import sbt.Task
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
      chuckPublishSnapshot := {
        val developmentLambdaConfiguration = chuckPublishConfig.value
        import developmentLambdaConfiguration._
        val lambda = resolvedLambdaForPublishing.value

        val maybeVpcConfig = maybeVpcConfigTask.value

        val lambdaSnapshot =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambdaSnapshot(
              lambda,
              jarStagingBucketName,
              PutFile(
                jarStagingS3KeyPrefix,
                codeGeneratorTask.value
              )
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logMessage(
            (Str("Just Published Snapshot as '") ++ Green(
              lambdaSnapshot.arn.value.toString
            ) ++ Str("'")).render
          )
        )

        lambdaSnapshot
      },
      chuckPublish := {
        val developmentLambdaConfiguration = chuckPublishConfig.value
        import developmentLambdaConfiguration._
        val lambda = resolvedLambdaForPublishing.value
        val code   = codeGeneratorTask.value

        streams.value.log.info(
          logMessage(
            (Str("About to upload '") ++ Green(
              code.toURI.toString
            ) ++ Str("' as AWS Lambda")).render
          )
        )

        val publishedLambda =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambda(
              lambda,
              jarStagingBucketName,
              PutFile(
                jarStagingS3KeyPrefix,
                code
              )
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logMessage(
            (Str("Just Published Version '") ++ Green(
              publishedLambda.version.value.toString
            ) ++ Str(
              "' as '"
            ) ++ Green(publishedLambda.arn.value) ++ Str("'")).render
          )
        )

        publishedLambda
      },
      chuckPublishTo := {
        val toAliasName = environmentArgParser.value.parsed

        val publishedLambda = chuckPublish.value

        val alias =
          com.itv.chuckwagon.deploy
            .aliasPublishedLambda(
              publishedLambda,
              toAliasName
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

  private def resolvedLambdaForPublishing: Def.Initialize[Task[Lambda]] = Def.taskDyn {
    val developmentLambdaConfiguration = chuckPublishConfig.value
    import developmentLambdaConfiguration._

    val maybeVpcConfig = maybeVpcConfigTask.value

    Def.task {
      Lambda(
        deployment = LambdaDeploymentConfiguration(
          name = chuckName.value,
          roleARN = chuckRoleTask.value.arn,
          vpcConfig = maybeVpcConfig
        ),
        runtime = lambdaRuntimeConfiguration
      )
    }
  }

  private def maybeVpcConfigTask: Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    BaseHelpers.maybeVpcConfig(chuckPublishConfig.value.vpcConfigDeclaration)
  }
  private def codeGeneratorTask: Def.Initialize[Task[File]] = Def.taskDyn {
    chuckPublishConfig.value.codeFile
  }

  private def chuckRoleTask: Def.Initialize[Task[Role]] = Def.taskDyn {
    Def.task {
      com.itv.chuckwagon.deploy
        .getPredefinedOrChuckwagonRole(
          chuckPublishConfig.value.roleARN,
          chuckName.value
        )
        .foldMap(chuckSDKFreeCompiler.value.compiler)
    }
  }
}
