package com.itv.sbt

import cats.data.NonEmptyList
import com.amazonaws.regions.Regions
import com.itv.aws.ARN
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.chuckwagon.deploy.AWSCompiler
import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import scala.concurrent.duration._
import fansi._
import fansi.Color._

import scala.collection.immutable.Seq

object AWSLambdaPlugin extends AutoPlugin {

  override def requires = sbtassembly.AssemblyPlugin

  override def trigger = allRequirements

  // Shouldn't be called on public API.
  val chuckEnvironments = settingKey[NonEmptyList[Environment]](
    "The environments through which our lambda will be promoted and tested"
  )

  object autoImport {
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
    val chuckLambda = settingKey[Lambda]("Lambda definition to be managed")
    def chuckDefineLambda(name: String,
                          roleARN: String,
                          handler: String,
                          timeoutInSeconds: Int,
                          memorySizeInMB: Int): Lambda = {

      require(
        timeoutInSeconds > 0 && timeoutInSeconds <= 300,
        "Lambda timeout must be between 1 and 300 seconds"
      )

      require(
        memorySizeInMB >= 128 && memorySizeInMB <= 1536,
        "Lambda memory must be between 128 and 1536 MBs"
      )

      Lambda(
        name = LambdaName(name),
        LambdaConfiguration(
          roleARN = ARN(roleARN),
          handler = LambdaHandler(handler),
          timeout = timeoutInSeconds.seconds,
          memorySize = MemorySize(memorySizeInMB)
        )
      )
    }
    def chuckDefineEnvironments(
      firstEnvironmentName: String,
      subsequentEnvironmentsNames: String*
    ): Seq[Setting[_]] = {
      val environments = BlueGreenEnvironments(
        firstEnvironmentName,
        subsequentEnvironmentsNames: _*
      )
      val environmentsSettings =
        environments.toList.flatMap(testConfigurationSettingsFor)

      val promotions: List[_root_.sbt.Def.Initialize[Task[Unit]]] =
        environments.toList.zip(environments.tail).flatMap {
          case (from: Environment, to: Environment) => {
            val testTask: _root_.sbt.Def.Initialize[Task[Unit]] =
              (test in from.configuration).toTask
            val promoteTask: _root_.sbt.Def.Initialize[Task[Unit]] =
              chuckPromote
                .toTask(s" ${from.aliasName.value} ${to.aliasName.value}")
            List(testTask, promoteTask)
          }
        } ::: (test in environments.reverse.head.configuration).toTask :: Nil

      (chuckEnvironments := environments) :: (chuckReleaseSteps := promotions) :: environmentsSettings
    }
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
  }
  import autoImport._

  def testConfigurationSettingsFor(lambdaEnv: Environment): Seq[Setting[_]] = {
    val configuration = lambdaEnv.configuration

    Seq(
      logBuffered in configuration := false, // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
      parallelExecution in configuration := false
    ) ++ inConfig(configuration)(Defaults.testSettings)
  }

  override lazy val projectSettings = chuckDefineEnvironments("qa", "prd") ++
      Seq(
        chuckSDKFreeCompiler := new AWSCompiler(
          com.itv.aws.lambda.awsLambda(chuckLambdaRegion.value)
        ),
        chuckCurrentAliases := {
        val maybeAliases = com.itv.chuckwagon.deploy
          .listAliases(chuckLambda.value.name)
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
          .listPublishedLambdasWithName(chuckLambda.value.name)
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
        chuckPublish := {
        val publishedLambda =
          com.itv.chuckwagon.deploy
            .publishLambda(
              chuckLambda.value,
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
        alias.arn
      },
        chuckPromote := {
        val args = spaceDelimited("<arg>").parsed
        val promotedToAlias = com.itv.chuckwagon.deploy
          .promoteLambda(
            chuckLambda.value.name,
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
        chuckCleanUp := {
        val deletedAliases =
          com.itv.chuckwagon.deploy
            .deleteRedundantAliases(
              chuckLambda.value.name,
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
            .deleteRedundantPublishedLambdas(chuckLambda.value.name)
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logItemsMessage(
            "Deleted the following redundant lambda versions",
            deletedLambdaVersions.map(_.value.toString): _*
          )
        )
        ()
      },
        chuckRelease := Def.taskDyn {

        def sequential[B](
          tasks: Seq[_root_.sbt.Def.Initialize[Task[Unit]]],
          last: _root_.sbt.Def.Initialize[Task[B]]
        ): _root_.sbt.Def.Initialize[Task[B]] =
          tasks.toList match {
            case Nil => Def.task { last.value }
            case x :: xs =>
              Def.taskDyn {
                val _ = x.value
                sequential(xs, last)
              }
          }

        val x: List[_root_.sbt.Def.Initialize[Task[Unit]]] =
          chuckReleaseSteps.value
        val _ = chuckPublish.value
        sequential(x, x.reverse.head)
      }.value
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
