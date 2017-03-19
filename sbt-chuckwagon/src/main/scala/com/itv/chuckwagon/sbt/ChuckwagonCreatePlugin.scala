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

object ChuckwagonCreatePlugin extends AutoPlugin {

  override def requires = sbtassembly.AssemblyPlugin && com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Create
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckCreate := {
        val toAliasName = environmentArgParser.value.parsed

        val developmentLambdaConfiguration = chuckCreateConfig.value
        import developmentLambdaConfiguration._

        val maybeVpcConfig = maybeVpcConfigTask.value

        val lambda = Lambda(
          deployment = LambdaDeploymentConfiguration(
            name = chuckName.value,
            roleARN = chuckRoleTask.value.arn,
            vpcConfig = maybeVpcConfig
          ),
          runtime = lambdaRuntimeConfiguration
        )

        val alias =
          com.itv.chuckwagon.deploy
            .uploadAndPublishLambdaToAlias(
              lambda,
              jarStagingBucketName,
              PutFile(
                jarStagingS3KeyPrefix,
                codeGeneratorTask.value
              ),
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

  private def maybeVpcConfigTask: Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    BaseHelpers.maybeVpcConfig(chuckCreateConfig.value.vpcConfigDeclaration)
  }
  private def codeGeneratorTask: Def.Initialize[Task[File]] = Def.taskDyn {
    chuckCreateConfig.value.codeFile
  }

  private def chuckRoleTask: Def.Initialize[Task[Role]] = Def.taskDyn {
    Def.task {
      com.itv.chuckwagon.deploy
        .getPredefinedOrChuckwagonRole(
          chuckCreateConfig.value.roleARN,
          chuckName.value
        )
        .foldMap(chuckSDKFreeCompiler.value.compiler)
    }
  }
}
