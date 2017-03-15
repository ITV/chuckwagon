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

    val chuckSDKFreeCompiler = settingKey[AWSCompiler](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )

    val chuckEnvironments = settingKey[NonEmptyList[Environment]](
      "The environments through which our lambda will be promoted and tested"
    )
    def chuckDefineEnvironments(environments: String*): NonEmptyList[Environment] = {
      NonEmptyList.of[String](environments.head, environments.tail: _*).map(Environment)
    }

    val chuckLambdaRegion =
      settingKey[Regions]("AWS region within which to manage Lambda")
    def chuckDefineRegion(region: String): Regions = {
      Regions.fromName(region)
    }

    val chuckLambdaName = settingKey[String]("The name of the Lambda.")

    val chuckCurrentAliases = taskKey[Option[List[Alias]]](
      "The Aliases currently configured in AWS (if Lambda exists)"
    )
    val chuckCurrentPublishedLambdas = taskKey[Option[List[PublishedLambda]]](
      "The currently published versions of this Lambda (if Lambda exists)"
    )

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
    val chuckRemoveLambdaTrigger =
      inputKey[Unit]("Remove invoke cron trigger for Lambda")

    def chuckVpcBuilder = VpcConfigDeclarationBuilder()
  }
  object Base extends Base

  trait Development {

    val chuckPublishTo =
      inputKey[Unit]("Upload latest code to Lambda and Publish it")

    val chuckDevConfig =
      settingKey[DevelopmentLambdaConfiguration]("Configuration for publishing")

    def chuckDevConfigBuilder = DevelopmentLambdaConfigurationBuilder()

  }

  object Development extends Development

  trait Production {

    val chuckProdConfig =
      settingKey[ProductionLambdaConfiguration]("Configuration for publishing")

    val chuckCopyDev =
      inputKey[Unit]("Upload latest code to Lambda and Publish it")

    def chuckProdConfigBuilder = ProductionLambdaConfigurationBuilder()
  }

  object Production extends Production

}
