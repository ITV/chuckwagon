package com.itv.chuckwagon.sbt

import cats.data.NonEmptyList
import com.amazonaws.regions.Regions
import com.itv.aws.ec2.Filter
import com.itv.aws.lambda._
import com.itv.aws.s3.{BucketName, S3Address, S3KeyPrefix}
import com.itv.chuckwagon.deploy.AWSCompiler
import sbt._

import scala.concurrent.duration._

object Keys {

  trait Base {

    val chuckSDKFreeCompiler = settingKey[AWSCompiler](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )

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

    val chuckCurrentAliases = taskKey[Option[List[Alias]]](
      "The Aliases currently configured in AWS (if Lambda exists)"
    )
    val chuckCurrentPublishedLambdas = taskKey[Option[List[PublishedLambda]]](
      "The currently published versions of this Lambda (if Lambda exists)"
    )

    val chuckVpcConfig = taskKey[Option[VpcConfig]](
      "Lookup desired vpn config for sbt defined VpcConfigDeclaration"
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
  }

  trait Publish {

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


    val chuckPublish =
      taskKey[Unit]("Upload latest code to Lambda and Publish it")
  }


  trait Copy {

  }

}
