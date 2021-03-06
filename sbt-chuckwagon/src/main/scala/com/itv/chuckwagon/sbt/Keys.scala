package com.itv.chuckwagon.sbt

import cats.Id
import cats.~>
import cats.data.NonEmptyList
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.itv.aws.iam.ARN
import com.itv.aws.lambda._
import com.itv.chuckwagon.deploy.AWSCompiler
import com.itv.chuckwagon.deploy.DeployLambdaA
import com.itv.chuckwagon.deploy.VpcConfigLookup
import com.itv.chuckwagon.sbt.builder._
import sbt._

case class Publish(roleARN: ARN, vpcConfigDeclaration: VpcConfigLookup)

object Keys {

  trait Base {
    import scala.language.implicitConversions

    val chuckAWSCredentialsProvider = settingKey[AWSCredentialsProvider](
      "The AWS Credentials Lookup Mechanism. Defaults to standard SDK chain."
    )

    val chuckSDKFreeCompiler = settingKey[DeployLambdaA ~> Id](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )

    val chuckEnvironments = settingKey[Set[Environment]](
      "(Optional) The Environments into which the the AWS Lambda should be published, copied and/or promoted (known as Aliases in the AWS Console)"
    )
    implicit def getChuckEnvironmentsFor(environments: Set[String]): Set[Environment] =
      environments.map(Environment)

    val chuckRegion =
      settingKey[Regions]("The AWS Region within which to create/update the AWS Lambda")

    implicit def getChuckRegionFor(regionName: String): Regions = Regions.fromName(regionName)

    val chuckNames = settingKey[List[LambdaName]]("The derived list of AWS Lambda Names")

    implicit def getChuckNamesFor(names: List[String]): List[LambdaName] = names.map(LambdaName)

    val chuckCurrentEnvironments = taskKey[Option[List[Alias]]](
      "The Environments (AWS Aliases) currently configured (if it exists)"
    )
    val chuckCurrentlyPublished = taskKey[Option[List[PublishedLambda]]](
      "The currently published versions of this AWS Lambda (if it exists)"
    )

    val chuckPromote =
      inputKey[Unit](
        """Promote the AWS Lambda Version in the first Environment (AWS Alias) to the second. Environments must exist in chuckEnvironments Setting, but associated Aliases will be created in AWS if they are missing. eg "chuckPromote blue-qa qa""""
      )
    val chuckCleanUp =
      taskKey[Unit](
        "Remove all AWS Lambda Versions not deployed to an Environment (AWS Alias) and all Environments not defined in chuckEnvironments Setting"
      )

    val chuckSetLambdaTrigger =
      inputKey[Unit](
        """Schedule AWS Lambda to be invoked based on a cron expression eg 'chuckSetLambdaTrigger qa "rate(1 minute)"'"""
      )
    val chuckRemoveLambdaTrigger =
      inputKey[Unit]("Remove Scheduled execution associated with AWS Lambda")

    def chuckVpcUsingFiltersBuilder = VpcConfigUsingFiltersLookupBuilder()
    def chuckVpcUsingIdsBuilder     = VpcConfigUsingIdsLookupBuilder()

    val chuckInvoke =
      inputKey[String](
        "Invoke the Lambda. Using the snapshot if no argument passed. Otherwise the environment or version"
      )
  }
  object Base extends Base

  trait Publish {

    val chuckPublishSnapshot =
      taskKey[List[LambdaSnapshot]]("Upload latest code to Lambda")

    val chuckPublish =
      taskKey[List[PublishedLambda]]("Upload latest code to Lambda and Publish it")

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
