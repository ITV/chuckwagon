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
      chuckNames := chuckCopyConfig.value.lambdaNames,
      chuckCopyFromOtherAccountTo := {
        val (fromAliasNameString, toAliasName) =
          (token(' ') ~> token(NotQuoted) ~ environmentArgParser.value).parsed

        val prodLambdaConfiguration = chuckCopyConfig.value
        import prodLambdaConfiguration._

        val maybeVpcConfig = maybeVpcConfigFromProductionLambdaConfiguration().value

        def logStream(msg: String): Unit = streams.value.log.info(msg)

        maybeVpcConfig.foreach { vpcConfig =>
          logStream(
            logMessage(
              (Str("Desired vpc-id: '") ++ Green(vpcConfig.vpc.id) ++ Str("' subnets: '") ++ vpcConfig.subnets
                .map(s => Green(s.id).render)
                .mkString(Str(", ").render) ++ Str("' security groups: '") ++ vpcConfig.securityGroups
                .map(sg => Green(sg.id).render)
                .mkString(Str(", ").render) ++ Str("'")).render
            )
          )
        }

        val credentials =
          com.itv.chuckwagon.deploy
            .assumeRole(
              ARN(assumableDevAccountRoleARN.value),
              AssumeRoleSessionName("chuckwagon-production-deployment")
            )
            .foldMap(chuckSDKFreeCompiler.value)

        val assumedDevelopmentRoleCompiler = new AWSCompiler(
          region = chuckRegion.value,
          credentials = credentials
        )

        // FIXME: Needs to download every Lambda Configuration (Handlers are all different)
        // https://github.com/ITV/chuckwagon/issues/1
        val downloadableDevelopmentPublishedLambda: DownloadablePublishedLambda =
          com.itv.chuckwagon.deploy
            .getDownloadablePublishedLambdaVersion(
              chuckNames.value.head,
              AliasName(fromAliasNameString)
            )
            .foldMap(assumedDevelopmentRoleCompiler.compiler)

        val httpClient = HttpClients.createDefault()
        val developmentLambdaCodeEntity =
          httpClient
            .execute(new HttpGet(downloadableDevelopmentPublishedLambda.downloadableLocation.value))
            .getEntity
        val developmentLambdaInputStream = developmentLambdaCodeEntity.getContent

        val productionLambdas =
          chuckNames.value.map { chuckName =>
            downloadableDevelopmentPublishedLambda.publishedLambda.lambda
              .copy(
                deployment = LambdaDeploymentConfiguration(
                  name = chuckName,
                  roleARN = chuckRoleTask.value.arn,
                  vpcConfig = maybeVpcConfig
                )
              )
          }

        try {
          val publishedLambdas =
            com.itv.chuckwagon.deploy
              .uploadAndPublishLambdas(
                productionLambdas,
                jarStagingBucketName,
                PutInputStream(
                  S3Key(s"${jarStagingS3KeyPrefix.value}${name.value}-copy.jar"),
                  developmentLambdaInputStream,
                  developmentLambdaCodeEntity.getContentLength
                )
              )
              .foldMap(chuckSDKFreeCompiler.value)

          publishedLambdas.foreach {
            publishedLambda =>
              val alias =
                com.itv.chuckwagon.deploy
                  .aliasPublishedLambda(
                    publishedLambda,
                    toAliasName
                  )
                  .foldMap(chuckSDKFreeCompiler.value)

              logStream(
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
        } finally {
          developmentLambdaInputStream.close()
        }
      }
    )

  def maybeVpcConfigFromProductionLambdaConfiguration(): Def.Initialize[Task[Option[VpcConfig]]] =
    Def.taskDyn {
      BaseHelpers.maybeVpcConfig(chuckCopyConfig.value.vpcConfigLookup)
    }

  private def chuckRoleTask: Def.Initialize[Task[Role]] = Def.taskDyn {
    Def.task {
      com.itv.chuckwagon.deploy
        .getPredefinedOrChuckwagonRole(
          chuckCopyConfig.value.roleARN,
          LambdaRoles.roleNameFor(name.value)
        )
        .foldMap(chuckSDKFreeCompiler.value)
    }
  }
}
