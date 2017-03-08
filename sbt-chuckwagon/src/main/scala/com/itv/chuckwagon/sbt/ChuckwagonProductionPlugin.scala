package com.itv.chuckwagon.sbt

import com.itv.aws.iam.ARN
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.aws.sts.AssumeRoleSessionName
import com.itv.chuckwagon.deploy.AWSCompiler
import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import com.itv.chuckwagon.sbt.LoggingUtils.logMessage
import fansi.Color.Green
import fansi.Str
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import sbt._
import sbt.Keys._
import complete.DefaultParsers._

object ChuckwagonProductionPlugin extends AutoPlugin {

  override def requires = com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Production

  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckEnvironments := chuckDefineEnvironments("blue-prd", "prd"),
      chuckPublishCopyFrom := {
        val args: Seq[String] = spaceDelimited("<arg>").parsed

        val credentials =
          com.itv.chuckwagon.deploy
            .assumeRole(
              ARN(chuckAssumableDevelopmentAccountRoleARN.value),
              AssumeRoleSessionName("chuckwagon-production-deployment")
            )
            .foldMap(new AWSCompiler(chuckLambdaRegion.value).compiler)

        val assumedDevelopmentRoleCompiler = new AWSCompiler(
          region = chuckLambdaRegion.value,
          credentials = Some(credentials)
        )

        val downloadableDevelopmentPublishedLambda: DownloadablePublishedLambda =
          com.itv.chuckwagon.deploy
            .getDownloadablePublishedLambdaVersion(
              LambdaName(chuckLambdaName.value),
              AliasName(args.head)
            )
            .foldMap(assumedDevelopmentRoleCompiler.compiler)

        val httpClient = HttpClients.createDefault()
        val developmentLambdaCodeEntity =
          httpClient.execute(new HttpGet(downloadableDevelopmentPublishedLambda.downloadableLocation.value)).getEntity
        val developmentLambdaInputStream = developmentLambdaCodeEntity.getContent

        val productionLambda =
          downloadableDevelopmentPublishedLambda.publishedLambda.lambda
            .copy(deployment = chuckDeploymentConfiguration.value)

        try {
          val alias =
            com.itv.chuckwagon.deploy
              .uploadAndPublishLambdaToAlias(
                productionLambda,
                BucketName(chuckJarStagingBucketName.value),
                PutInputStream(
                  S3Key(s"${chuckJarStagingBucketKeyPrefix.value}${chuckLambdaName.value}-copy.jar"),
                  developmentLambdaInputStream,
                  developmentLambdaCodeEntity.getContentLength
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
        } finally {
          developmentLambdaInputStream.close()
        }
      }
    )
}
