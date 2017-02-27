package com.itv.sbt

import cats.data.NonEmptyList
import com.amazonaws.regions.Regions
import com.itv.aws.ARN
import com.itv.aws.ec2.Filter
import com.itv.aws.events.ScheduleExpression
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.chuckwagon.deploy.AWSCompiler
import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import scala.concurrent.duration._
import fansi._
import fansi.Color._
import complete.DefaultParsers._
import sbt.complete._

import scala.collection.immutable.Seq

object AWSLambdaPlugin extends AutoPlugin {

  override def requires = sbtassembly.AssemblyPlugin

  object autoImport {
    val chuckEnvironments = settingKey[NonEmptyList[Environment]](
      "The environments through which our lambda will be promoted and tested"
    )
    def chuckDefineEnvironments(environments: String*):NonEmptyList[Environment] = {
      NonEmptyList.of[String](environments.head, environments.tail :_*).map(Environment)
    }

    val chuckLambdaRegion =
      settingKey[Regions]("AWS region within which to manage Lambda")
    def chuckDefineRegion(region: String): Regions = {
      Regions.fromName(region)
    }
    val chuckStagingS3Address = settingKey[S3Address](
      "The S3 address we want to use for staging our Scala Assembly JAR for Lambda create/update"
    )
    def chuckDefineS3Address(bucketName: String,
                             keyPrefix: String): S3Address = {
      S3Address(BucketName(bucketName), S3KeyPrefix(keyPrefix))
    }
    val chuckLambdaDeclaration = settingKey[LambdaDeclaration]("Lambda declaration definition to be managed")
    def chuckDefineLambdaDeclaration(name: String,
                                     handler: String,
                                     timeoutInSeconds: Int,
                                     memorySizeInMB: Int): LambdaDeclaration = {

      require(
        timeoutInSeconds > 0 && timeoutInSeconds <= 300,
        "Lambda timeout must be between 1 and 300 seconds"
      )

      require(
        memorySizeInMB >= 128 && memorySizeInMB <= 1536,
        "Lambda memory must be between 128 and 1536 MBs"
      )

      LambdaDeclaration(
        name = LambdaName(name),
        handler = LambdaHandler(handler),
        timeout = timeoutInSeconds.seconds,
        memorySize = MemorySize(memorySizeInMB)
      )
    }
    val chuckVpnConfigDeclaration = settingKey[Option[VpcConfigDeclaration]](
      "Optional VPN Configuration Lookup Parameters"
    )
    def chuckDefineVpnConfigDeclaration(
                                         vpcLookupFilters: List[(String, String)],
                                         subnetsLookupFilters: List[(String, String)],
                                         securityGroupsLookupFilters: List[(String, String)]
                                       ): Option[VpcConfigDeclaration] = {
      Option(VpcConfigDeclaration(
        vpcLookupFilters =
          vpcLookupFilters.map(t => Filter(t._1, t._2)),
        subnetsLookupFilters =
          subnetsLookupFilters.map(t => Filter(t._1, t._2)),
        securityGroupsLookupFilters =
          securityGroupsLookupFilters.map(t => Filter(t._1, t._2))
      ))
    }
    val chuckVpcConfig = taskKey[Option[VpcConfig]](
      "Lookup desired vpn config for sbt defined VpcConfigDeclaration"
    )

    val chuckSDKFreeCompiler = settingKey[AWSCompiler](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )

    val chuckCurrentAliases = taskKey[Option[List[Alias]]](
      "The Aliases currently configured in AWS (if Lambda exists)"
    )
    val chuckCurrentPublishedLambdas = taskKey[Option[List[PublishedLambda]]](
      "The currently published versions of this Lambda (if Lambda exists)"
    )

    val chuckPublish =
      taskKey[Unit]("Upload latest code to Lambda and Publish it")
    val chuckPromote =
      inputKey[Unit]("Promote a published Lambda by attaching it to an alias")
    val chuckCleanUp =
      taskKey[Unit]("Remove all unused Published Lambda Versions and Aliases")

    val chuckReleaseSteps =
      settingKey[List[_root_.sbt.Def.Initialize[Task[Unit]]]]("")
    val chuckRelease =
      taskKey[Unit]("Run the entire Deployment Pipeline")
    val chuckSetLambdaTrigger =
      inputKey[Unit]("Schedule Lambda to be invoked based on a cron expression")
  }
  import autoImport._

  def testConfigurationSettingsFor(lambdaEnv: Environment): Seq[Setting[_]] = {
    val configuration = lambdaEnv.configuration

    Seq(
      logBuffered in configuration := false, // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
      parallelExecution in configuration := false
    ) ++ inConfig(configuration)(Defaults.testSettings)
  }

  val chuckSetLambdaTriggerArgsParser =
    Def.setting {
      val environments: Seq[String] = chuckEnvironments.value.map(_.name).toList
      val x: Parser[String] = token(NotQuoted.examples(FixedSetExamples(environments)))
      (token(' ') ~> x) ~ (token(' ') ~> token(StringBasic))
    }

  override lazy val projectSettings =
      Seq(
        chuckEnvironments := chuckDefineEnvironments("blue-qa", "qa", "blue-prd", "prd"),
        chuckVpnConfigDeclaration := None,
        chuckSDKFreeCompiler := new AWSCompiler(
          com.itv.aws.lambda.awsLambda(chuckLambdaRegion.value)
        ),
        chuckCurrentAliases := {
        val maybeAliases = com.itv.chuckwagon.deploy
          .listAliases(chuckLambdaDeclaration.value.name)
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
          .listPublishedLambdasWithName(chuckLambdaDeclaration.value.name)
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
        chuckVpcConfig := {
          val maybeVpcConfig =
            com.itv.chuckwagon.deploy
              .getVpcConfig(
                chuckVpnConfigDeclaration.value
              )
              .foldMap(chuckSDKFreeCompiler.value.compiler)

          maybeVpcConfig match {
            case Some(vpcConfig) => {
              streams.value.log.info(logMessage(
                (Str("Desired vpc-id: '") ++ Green(vpcConfig.vpc.id) ++ Str("' subnets: '") ++ vpcConfig.subnets.map(s => Green(s.id).render).mkString(Str(", ").render) ++ Str("' security groups: '") ++ vpcConfig.securityGroups.map(sg => Green(sg.id).render).mkString(Str(", ").render) ++ Str("'")).render
              ))
            }
            case None =>
              streams.value.log.info(logMessage(
                "No vpcConfigDeclaration defined so cannot lookup vpcConfig"
              ))
          }
          maybeVpcConfig
        },
        chuckPublish := {
          val maybeVpcConfig = chuckVpcConfig.value
          val lambdaDeclaration = maybeVpcConfig match {
            case Some(vpcConfig) => chuckLambdaDeclaration.value.copy(vpcConfig = Option(vpcConfig))
            case None => chuckLambdaDeclaration.value
          }

          val publishedLambda =
            com.itv.chuckwagon.deploy
              .publishLambda(
                lambdaDeclaration,
                chuckStagingS3Address.value,
                sbtassembly.AssemblyKeys.assembly.value
              )
              .foldMap(chuckSDKFreeCompiler.value.compiler)

          val alias =
            com.itv.chuckwagon.deploy
              .aliasPublishedLambda(
                publishedLambda,
                chuckEnvironments.value.head.aliasName
              )
              .foldMap(chuckSDKFreeCompiler.value.compiler)

          streams.value.log.info(
            logMessage(
              (Str("Just Published Version '") ++ Green(
                publishedLambda.version.value.toString
              ) ++ Str("' to Environment '") ++ Green(alias.name.value) ++ Str(
                "' as '"
              ) ++ Green(alias.arn.value) ++ Str("'")).render
            )
          )

        ()
      },
        chuckPromote := {
        val args = spaceDelimited("<arg>").parsed
        val promotedToAlias = com.itv.chuckwagon.deploy
          .promoteLambda(
            chuckLambdaDeclaration.value.name,
            AliasName(args(0)),
            AliasName(args(1))
          )
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logMessage(
            (Str("Just Promoted Version '") ++ Green(
              promotedToAlias.lambdaVersion.value.toString
            ) ++ Str("' from Environment '") ++ Green(args(0)) ++ Str(
              "' to Environment '"
            ) ++ Green(args(1)) ++ Str("' as '") ++ Green(
              promotedToAlias.arn.value
            ) ++ Str("'")).render
          )
        )
        ()
      },
        chuckSetLambdaTrigger := {
          val (aliasString, scheduleExpressionString) =
            chuckSetLambdaTriggerArgsParser.parsed

          val maybeAliases = com.itv.chuckwagon.deploy
            .listAliases(chuckLambdaDeclaration.value.name)
            .foldMap(chuckSDKFreeCompiler.value.compiler)

          maybeAliases.getOrElse(Nil).find(alias => alias.name.value == aliasString) match {
            case Some(alias) => {
              val _ = com.itv.chuckwagon.deploy
                .setLambdaTrigger(alias, ScheduleExpression(scheduleExpressionString))
                .foldMap(chuckSDKFreeCompiler.value.compiler)

              streams.value.log.info(
                logMessage(
                  (Str("Just Created Schedule Trigger for Environment '") ++ Green(alias.name.value) ++ Str("' with Expression '") ++ Green(scheduleExpressionString) ++ Str("'")).render
                )
              )
            }
            case None =>
              throw new Exception(s"Cannot set Lambda Trigger on '${chuckLambdaDeclaration.value.name.value}' because '${aliasString}' does not exist yet.")
          }
          ()
        },
        chuckCleanUp := {
        val deletedAliases =
          com.itv.chuckwagon.deploy
            .deleteRedundantAliases(
              chuckLambdaDeclaration.value.name,
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
            .deleteRedundantPublishedLambdas(chuckLambdaDeclaration.value.name)
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

  def logItemsMessage(prefix: String, items: String*): String = {

    val colouredItems =
      if (items.isEmpty) ""
      else
        items
          .map(Green(_).render)
          .mkString(Str("'").render, Str("', '").render, Str("'").render)

    (Cyan("Chuckwagon") ++ Str(s": $prefix ")).render ++ colouredItems
  }

  def logMessage(message: String): String = {
    (Cyan("Chuckwagon") ++ Str(s": ")).render ++ message
  }

}
