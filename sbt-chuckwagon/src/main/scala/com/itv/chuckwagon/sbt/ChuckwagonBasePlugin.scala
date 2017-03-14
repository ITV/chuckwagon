package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.deploy.AWSCompiler
import fansi.Color.Green
import fansi.Str
import sbt._
import sbt.Keys._
import LoggingUtils._
import com.itv.aws.iam.ARN
import com.itv.aws.lambda._
import Parsers._
import com.itv.aws.events.ScheduleExpression
import sbt.complete.DefaultParsers.{token, StringBasic}

object ChuckwagonBasePlugin extends AutoPlugin {

  object autoImport extends Keys.Base
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckSDKFreeCompiler := new AWSCompiler(chuckLambdaRegion.value),
      chuckRoleARN := None,
      chuckVpnConfigDeclaration := None,
      chuckVpcConfig := {
        val maybeVpcConfig =
          com.itv.chuckwagon.deploy
            .getVpcConfig(
              chuckVpnConfigDeclaration.value
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybeVpcConfig match {
          case Some(vpcConfig) => {
            sLog.value.info(
              logMessage(
                (Str("Desired vpc-id: '") ++ Green(vpcConfig.vpc.id) ++ Str("' subnets: '") ++ vpcConfig.subnets
                  .map(s => Green(s.id).render)
                  .mkString(Str(", ").render) ++ Str("' security groups: '") ++ vpcConfig.securityGroups
                  .map(sg => Green(sg.id).render)
                  .mkString(Str(", ").render) ++ Str("'")).render
              ))
          }
          case None =>
            sLog.value.info(
              logMessage(
                "No vpcConfigDeclaration defined so cannot lookup vpcConfig"
              ))
        }
        maybeVpcConfig
      },
      chuckRole := {
        com.itv.chuckwagon.deploy
          .getPredefinedOrChuckwagonRole(
            chuckRoleARN.value.map(ARN),
            LambdaName(chuckLambdaName.value)
          )
          .foldMap(chuckSDKFreeCompiler.value.compiler)
      },
      chuckDeploymentConfiguration := {
        LambdaDeploymentConfiguration(LambdaName(chuckLambdaName.value), chuckRole.value.arn, chuckVpcConfig.value)
      },
      chuckJarStagingBucketKeyPrefix := "",
      chuckPromote := {
        val (fromAliasName, toAliasName) =
          (environmentArgParser.value ~ environmentArgParser.value).parsed
        val promotedToAlias = com.itv.chuckwagon.deploy
          .promoteLambda(
            chuckDeploymentConfiguration.value.name,
            fromAliasName,
            toAliasName
          )
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logMessage(
            (Str("Just Promoted Version '") ++ Green(
              promotedToAlias.lambdaVersion.value.toString
            ) ++ Str("' from Environment '") ++ Green(fromAliasName.value) ++ Str(
              "' to Environment '"
            ) ++ Green(toAliasName.value) ++ Str("' as '") ++ Green(
              promotedToAlias.arn.value
            ) ++ Str("'")).render
          )
        )
        ()
      },
      chuckSetLambdaTrigger := {
        val (targetAliasName, scheduleExpressionString) =
          (environmentArgParser.value ~ (token(' ') ~> token(StringBasic))).parsed

        val maybeAliases = com.itv.chuckwagon.deploy
          .listAliases(chuckDeploymentConfiguration.value.name)
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybeAliases.getOrElse(Nil).find(alias => alias.name == targetAliasName) match {
          case Some(alias) => {
            val _ = com.itv.chuckwagon.deploy
              .setLambdaTrigger(alias, ScheduleExpression(scheduleExpressionString))
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            streams.value.log
              .info(
                logMessage(
                  (Str("Just Created Schedule Trigger for Environment '") ++ Green(alias.name.value) ++ Str(
                    "' with Expression '") ++ Green(scheduleExpressionString) ++ Str("'")).render
                )
              )
          }
          case None =>
            throw new Exception(
              s"Cannot set Lambda Trigger on '${chuckDeploymentConfiguration.value.name.value}' because '${targetAliasName.value}' does not exist yet.")
        }
        ()
      },
      chuckRemoveLambdaTrigger := {
        val targetAliasName = environmentArgParser.value.parsed

        val maybeAliases = com.itv.chuckwagon.deploy
          .listAliases(chuckDeploymentConfiguration.value.name)
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybeAliases.getOrElse(Nil).find(alias => alias.name == targetAliasName) match {
          case Some(alias) => {

            com.itv.chuckwagon.deploy
              .removeLambdaTrigger(alias)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            streams.value.log
              .info(
                logMessage(
                  (Str("Just Removed Schedule Trigger for Environment '") ++ Green(alias.name.value) ++ Str("'")).render
                )
              )
          }
          case None =>
            throw new Exception(
              s"Cannot remove Lambda Trigger on '${chuckDeploymentConfiguration.value.name.value}' because '${targetAliasName.value}' does not exist yet.")
        }
        ()
      },
      chuckCurrentAliases := {
        val maybeAliases = com.itv.chuckwagon.deploy
          .listAliases(chuckDeploymentConfiguration.value.name)
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybeAliases match {
          case Some(aliases) => {
            streams.value.log.info(
              logItemsMessage(
                "Current Aliases and associated Lambda Versions",
                aliases.map(
                  alias => s"${alias.name.value}=${alias.lambdaVersion.value}"
                ): _*
              )
            )
          }
          case None =>
            streams.value.log
              .info(logItemsMessage("No Lambda defined so no aliases exist"))
        }
        maybeAliases
      },
      chuckCurrentPublishedLambdas := {
        val maybePublishedLambdas = com.itv.chuckwagon.deploy
          .listPublishedLambdasWithName(chuckDeploymentConfiguration.value.name)
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybePublishedLambdas match {
          case Some(publishedLambdas) => {
            streams.value.log.info(
              logItemsMessage(
                "Currently published versions",
                publishedLambdas.map(_.version.value.toString): _*
              )
            )
          }
          case None =>
            streams.value.log.info(
              logItemsMessage(
                "No Lambda defined so no published versions exist"
              )
            )
        }

        maybePublishedLambdas
      },
      chuckCleanUp := {
        val deletedAliases =
          com.itv.chuckwagon.deploy
            .deleteRedundantAliases(
              chuckDeploymentConfiguration.value.name,
              chuckEnvironments.value.toList.map(_.aliasName)
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logItemsMessage(
            "Deleted the following redundant aliases",
            deletedAliases.map(_.value): _*
          )
        )

        val deletedLambdaVersions =
          com.itv.chuckwagon.deploy
            .deleteRedundantPublishedLambdas(chuckDeploymentConfiguration.value.name)
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logItemsMessage(
            "Deleted the following redundant lambda versions",
            deletedLambdaVersions.map(_.value.toString): _*
          )
        )
        ()
      }
    )
}
