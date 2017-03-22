package com.itv.chuckwagon.sbt

import com.itv.aws.iam.ARN
import com.itv.aws.iam.Role
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.aws.sts.AssumeRoleSessionName
import com.itv.chuckwagon.deploy.AWSCompiler
import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import com.itv.chuckwagon.sbt.LoggingUtils.logMessage
import com.itv.chuckwagon.sbt.Parsers.environmentArgParser
import fansi.Color.Green
import fansi.Str
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import sbt._
import sbt.Keys._
import complete.DefaultParsers._

object ChuckwagonCopyPlugin extends AutoPlugin {

  override def requires = com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Copy

  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckCopyFromOtherAccountTo := {
        val (fromAliasNameString, toAliasName) =
          (token(' ') ~> token(NotQuoted) ~ environmentArgParser.value).parsed

        val prodLambdaConfiguration = chuckCopyConfig.value
        import prodLambdaConfiguration._

        val maybeVpcConfig = maybeVpcConfigFromProductionLambdaConfiguration().value

        val credentials =
          com.itv.chuckwagon.deploy
            .assumeRole(
              ARN(assumableDevAccountRoleARN.value),
              AssumeRoleSessionName("chuckwagon-production-deployment")
            )
            .foldMap(new AWSCompiler(chuckRegion.value).compiler)

        val assumedDevelopmentRoleCompiler = new AWSCompiler(
          region = chuckRegion.value,
          credentials = Some(credentials)
        )

        val downloadableDevelopmentPublishedLambda: DownloadablePublishedLambda =
          com.itv.chuckwagon.deploy
            .getDownloadablePublishedLambdaVersion(
              chuckName.value,
              AliasName(fromAliasNameString)
            )
            .foldMap(assumedDevelopmentRoleCompiler.compiler)

        val httpClient = HttpClients.createDefault()
        val developmentLambdaCodeEntity =
          httpClient
            .execute(new HttpGet(downloadableDevelopmentPublishedLambda.downloadableLocation.value))
            .getEntity
        val developmentLambdaInputStream = developmentLambdaCodeEntity.getContent

        val productionLambda =
          downloadableDevelopmentPublishedLambda.publishedLambda.lambda
            .copy(
              deployment = LambdaDeploymentConfiguration(
                name = chuckName.value,
                roleARN = chuckRoleTask.value.arn,
                vpcConfig = maybeVpcConfig
              )
            )

        try {
          val publishedLambda =
            com.itv.chuckwagon.deploy
              .uploadAndPublishLambda(
                productionLambda,
                jarStagingBucketName,
                PutInputStream(
                  S3Key(s"${jarStagingS3KeyPrefix.value}${chuckName.value}-copy.jar"),
                  developmentLambdaInputStream,
                  developmentLambdaCodeEntity.getContentLength
                )
              )
              .foldMap(chuckSDKFreeCompiler.value.compiler)

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
        } finally {
          developmentLambdaInputStream.close()
        }
      }
    )

  def maybeVpcConfigFromProductionLambdaConfiguration(): Def.Initialize[Task[Option[VpcConfig]]] =
    Def.taskDyn {
      BaseHelpers.maybeVpcConfig(chuckCopyConfig.value.vpcConfigDeclaration)
    }

  private def chuckRoleTask: Def.Initialize[Task[Role]] = Def.taskDyn {
    Def.task {
      com.itv.chuckwagon.deploy
        .getPredefinedOrChuckwagonRole(
          chuckCopyConfig.value.roleARN,
          chuckName.value
        )
        .foldMap(chuckSDKFreeCompiler.value.compiler)
    }
  }
}
