package com.itv.chuckwagon.sbt

import cats.data.NonEmptyList
import com.amazonaws.regions.Regions
import com.itv.aws.iam.ARN
import com.itv.aws.lambda._
import com.itv.chuckwagon.deploy.AWSCompiler
import com.itv.chuckwagon.sbt.builder._
import sbt._

case class Publish(roleARN: ARN, vpcConfigDeclaration: VpcConfigDeclaration)

object Keys {

  trait Base {
    import scala.language.implicitConversions

    val chuckSDKFreeCompiler = settingKey[AWSCompiler](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )

    val chuckEnvironments = settingKey[Set[Environment]](
      "The environments through which our lambda will be promoted and tested"
    )
    implicit def getChuckEnvironmentsFor(environments: Set[String]): Set[Environment] =
      environments.map(Environment)

    val chuckRegion =
      settingKey[Regions]("AWS region within which to manage Lambda")

    implicit def getChuckRegionFor(regionName: String): Regions = Regions.fromName(regionName)

    val chuckName = settingKey[LambdaName]("The name of the Lambda.")

    implicit def getChuckNameFor(lambdaName: String): LambdaName = LambdaName(lambdaName)

    val chuckCurrentAliases = taskKey[Option[List[Alias]]](
      "The Aliases currently configured in AWS (if Lambda exists)"
    )
    val chuckCurrentlyPublished = taskKey[Option[List[PublishedLambda]]](
      "The currently published versions of this Lambda (if Lambda exists)"
    )

    val chuckPromote =
      inputKey[Unit]("Promote a published Lambda by attaching it to an alias")
    val chuckCleanUp =
      taskKey[Unit](
        "Remove all Published Lambda Versions not attached to Aliases and all Aliases not defined in chuckEnvironments"
      )

    val chuckSetLambdaTrigger =
      inputKey[Unit]("Schedule Lambda to be invoked based on a cron expression")
    val chuckRemoveLambdaTrigger =
      inputKey[Unit]("Remove invoke cron trigger for Lambda")

    def chuckVpcBuilder = VpcConfigDeclarationBuilder()
  }
  object Base extends Base

  trait Publish {

    val chuckPublishSnapshot =
      taskKey[LambdaSnapshot]("Upload latest code to Lambda")

    val chuckPublish =
      taskKey[PublishedLambda]("Upload latest code to Lambda and Publish it")

    val chuckPublishTo =
      inputKey[Unit]("Upload latest code to Lambda and Publish it to an Environment")

    val chuckPublishConfig =
      settingKey[PublishLambdaConfiguration]("Configuration for publishing")

    def chuckPublishConfigBuilder = PublishLambdaConfigurationBuilder()

  }

  object Publish extends Publish

  trait Copy {

    val chuckCopyConfig =
      settingKey[CopyLambdaConfiguration]("Configuration for publishing")

    val chuckCopyFromOtherAccountTo =
      inputKey[Unit](
        "Copy Lambda from an environment on an Account where it was Published to an environment in this Account"
      )

    def chuckCopyConfigBuilder = CopyLambdaConfigurationBuilder()
  }

  object Copy extends Copy

}
