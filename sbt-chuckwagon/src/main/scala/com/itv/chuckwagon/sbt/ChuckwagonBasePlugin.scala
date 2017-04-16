package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.deploy.AWSCompiler
import fansi.Color.Green
import fansi.Str
import sbt._
import sbt.Keys._
import LoggingUtils._
import com.itv.aws.lambda._
import Parsers._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.itv.aws.events.ScheduleExpression
import sbt.complete.DefaultParsers._

object ChuckwagonBasePlugin extends AutoPlugin {

  object autoImport extends Keys.Base
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckAWSCredentialsProvider := DefaultAWSCredentialsProviderChain.getInstance,
      chuckEnvironments := Set[String](),
      chuckSDKFreeCompiler := new AWSCompiler(chuckRegion.value, chuckAWSCredentialsProvider.value),
      chuckPromote := {
        val (fromAliasName, toAliasName) =
          (environmentArgParser.value ~ environmentArgParser.value).parsed
        val promotedToAliases = com.itv.chuckwagon.deploy
          .promoteLambdas(
            chuckNames.value,
            fromAliasName,
            toAliasName
          )
          .foldMap(chuckSDKFreeCompiler.value.compiler)

        promotedToAliases.foreach {
          promotedToAlias =>
            streams.value.log.info(
              logMessage(
                promotedToAlias.lambdaName,
                (Str("Just Promoted Version '") ++ Green(
                  promotedToAlias.lambdaVersion.value.toString
                ) ++ Str("' from Environment '") ++ Green(fromAliasName.value) ++ Str(
                  "' to Environment '"
                ) ++ Green(toAliasName.value) ++ Str("' as '") ++ Green(
                  promotedToAlias.arn.value
                ) ++ Str("'")).render
              )
            )
        }
        ()
      },
      chuckSetLambdaTrigger := {
        val (targetAliasName, scheduleExpressionString) =
          (environmentArgParser.value ~ (token(' ') ~> token(StringBasic))).parsed

        chuckNames.value.foreach {
          chuckName =>
            val maybeAliases = com.itv.chuckwagon.deploy
              .listAliases(chuckName)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            maybeAliases.getOrElse(Nil).find(alias => alias.name == targetAliasName) match {
              case Some(alias) => {
                val _ = com.itv.chuckwagon.deploy
                  .setLambdaTrigger(alias, ScheduleExpression(scheduleExpressionString))
                  .foldMap(chuckSDKFreeCompiler.value.compiler)

                streams.value.log
                  .info(
                    logMessage(
                      chuckName,
                      (Str("Just Created Schedule Trigger for Environment '") ++ Green(alias.name.value) ++ Str(
                        "' with Expression '"
                      ) ++ Green(scheduleExpressionString) ++ Str("'")).render
                    )
                  )
              }
              case None =>
                throw new Exception(
                  s"Cannot set Lambda Trigger on '${chuckName.value}' because '${targetAliasName.value}' does not exist yet."
                )
            }
        }
        ()
      },
      chuckRemoveLambdaTrigger := {
        val targetAliasName = environmentArgParser.value.parsed

        chuckNames.value.foreach {
          chuckName =>
            val maybeAliases = com.itv.chuckwagon.deploy
              .listAliases(chuckName)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            maybeAliases.getOrElse(Nil).find(alias => alias.name == targetAliasName) match {
              case Some(alias) => {

                com.itv.chuckwagon.deploy
                  .removeLambdaTrigger(alias)
                  .foldMap(chuckSDKFreeCompiler.value.compiler)

                streams.value.log
                  .info(
                    logMessage(
                      chuckName,
                      (Str("Just Removed Schedule Trigger for Environment '") ++ Green(alias.name.value) ++ Str(
                        "'"
                      )).render
                    )
                  )
              }
              case None =>
                throw new Exception(
                  s"Cannot remove Lambda Trigger on '${chuckName.value}' because '${targetAliasName.value}' does not exist yet."
                )
            }
        }
        ()
      },
      chuckCurrentEnvironments := {
        chuckNames.value.headOption.flatMap {
          chuckName =>
            val maybeAliases = com.itv.chuckwagon.deploy
              .listAliases(chuckName)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            maybeAliases match {
              case Some(aliases) => {
                streams.value.log.info(
                  logItemsMessage(
                    chuckName,
                    "Current Aliases and associated Lambda Versions",
                    aliases.map(
                      alias => s"${alias.name.value}=${alias.lambdaVersion.value}"
                    ): _*
                  )
                )
              }
              case None =>
                streams.value.log
                  .info(logItemsMessage(chuckName, "No Lambda defined so no aliases exist"))
            }
            maybeAliases
        }
      },
      chuckCurrentlyPublished := {
        chuckNames.value.headOption.flatMap {
          chuckName =>
            val maybePublishedLambdas = com.itv.chuckwagon.deploy
              .listPublishedLambdasWithName(chuckName)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

            maybePublishedLambdas match {
              case Some(publishedLambdas) => {
                streams.value.log.info(
                  logItemsMessage(
                    chuckName,
                    "Currently published versions",
                    publishedLambdas.map(_.version.value.toString): _*
                  )
                )
              }
              case None =>
                streams.value.log.info(
                  logMessage(chuckName, "No Lambda defined so no published versions exist")
                )
            }
            maybePublishedLambdas
        }
      },
      chuckCleanUp := {

        chuckNames.value.foreach { chuckName =>
          val deletedAliases =
            com.itv.chuckwagon.deploy
              .deleteRedundantAliases(
                chuckName,
                chuckEnvironments.value.toList.map(_.aliasName)
              )
              .foldMap(chuckSDKFreeCompiler.value.compiler)

          streams.value.log.info(
            logItemsMessage(
              chuckName,
              s"Deleted the following redundant aliases",
              deletedAliases.map(_.value): _*
            )
          )
        }

        chuckNames.value.foreach { chuckName =>
          val deletedLambdaVersions =
            com.itv.chuckwagon.deploy
              .deleteRedundantPublishedLambdas(chuckName)
              .foldMap(chuckSDKFreeCompiler.value.compiler)

          streams.value.log.info(
            logItemsMessage(
              chuckName,
              s"Deleted the following redundant lambda versions ",
              deletedLambdaVersions.map(_.value.toString): _*
            )
          )
        }
        ()
      },
      chuckInvoke := {
        val env: (Option[LambdaName], Option[Either[AliasName, LambdaVersion]]) =
          (chuckNameParser.value.? ~ (environmentArgParser.value || versionArgParser.value).?).parsed

        val qualifier: Option[InvokeQualifier] = env._2.map {
          case Left(environment) => EnvironmentQualifier(environment)
          case Right(version)    => VersionQualifier(version)
        }

        val lambdaName: LambdaName = env._1 match {
          case Some(lambdaName) => lambdaName
          case None =>
            chuckNames.value match {
              case chuckName :: Nil     => chuckName
              case moreThanOneChuckName => throw new IllegalArgumentException("")
            }
        }

        streams.value.log.info(
          logItemsMessage(lambdaName, "About to invoke Lambda", qualifier.toList.map(_.toString): _*)
        )

        val output: LambdaResponse =
          com.itv.chuckwagon.deploy
            .invokeLambda(lambdaName, qualifier)
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        streams.value.log.info(
          logItemsMessage(lambdaName, "Result of running Lambda", output.toString)
        )

        output match {
          case res: LambdaResponsePayload => res.toString()
          case other                      => sys.error(other.toString())
        }
      }
    )
}
