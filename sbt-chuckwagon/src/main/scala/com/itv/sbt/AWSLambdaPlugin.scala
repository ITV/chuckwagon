package com.itv.sbt

import com.amazonaws.regions.Regions
import com.itv.aws.ARN
import com.itv.aws.lambda._
import com.itv.aws.s3.{Bucket, S3Key, S3Location}
import com.itv.chuckwagon.deploy.AWSCompiler
import sbt._
import sbt.complete.DefaultParsers._

import scala.concurrent.duration._

object AWSLambdaPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val awsLambdaRegion = settingKey[Regions](
      "AWS region within which to manage Lambda"
    )
    def awsRegion(region: String):Regions = {
      Regions.fromName(region)
    }
    val awsLambdaS3Location = settingKey[S3Location](
      "The S3 location where Scala Assembly JAR will be staged for Lambda create/update"
    )
    def awsS3Location(bucketName: String, keyPrefix: String):S3Location = {
      S3Location(Bucket(bucketName), S3Key(keyPrefix))
    }
    val awsLambda = settingKey[Lambda](
      "Lambda definition to be managed"
    )
    def awsLambdaAndConfiguration(name: String,
                                  roleARN: String,
                                  handler: String,
                                  timeoutInSeconds: Int,
                                  memorySizeInMB: Int):Lambda = {

      require(timeoutInSeconds > 0 && timeoutInSeconds <= 300, "Lambda timeout must be between 1 and 300 seconds")

      require(memorySizeInMB >= 128 && memorySizeInMB <= 1536, "Lambda memory must be between 128 and 1536 MBs")

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
    val awsSDKFreeCompiler = settingKey[AWSCompiler](
      "The Free Monad Compiler for our DeployLambdaA ADT"
    )
    val actualAliases = taskKey[List[Alias]](
      "The Aliases currently configured in AWS"
    )

    val awsLambdaDeploy = taskKey[ARN](
      "Deploy, test and put live the code in this project as an AWS Lambda"
    )
    val awsLambdaPromote = inputKey[Unit](
      "Promote a published Lambda by attaching it to an alias"
    )
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    awsSDKFreeCompiler := new AWSCompiler(com.itv.aws.lambda.awsLambda(awsLambdaRegion.value)),
    actualAliases := {
      com.itv.chuckwagon.deploy.listAliases(awsLambda.value.name).foldMap(awsSDKFreeCompiler.value.compiler)
    }
//    awsLambdaPromote := {
//      val args: Seq[String] = spaceDelimited("<arg>").parsed
//      com.itv.chuckwagon.deploy.promoteLambda().foldMap(awsSDKFreeCompiler.value)
//      ()
//    }
  )
}
