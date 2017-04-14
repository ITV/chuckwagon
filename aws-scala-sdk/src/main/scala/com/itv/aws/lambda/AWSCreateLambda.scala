package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.CreateFunctionRequest
import com.amazonaws.services.lambda.model.CreateFunctionResult
import com.amazonaws.services.lambda.model.DeadLetterConfig
import com.amazonaws.services.lambda.model.FunctionCode
import com.amazonaws.services.lambda.model.{VpcConfig => AWSVpcConfig}
import com.itv.aws.s3.S3Location
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._

class AWSCreateLambdaSnapshot(awsLambda: AWSLambda) {
  def apply(lambda: Lambda, s3Location: S3Location): LambdaSnapshot = {
    val awsCreateFunctionResponse = AWSCreateLambda.createLambda(awsLambda, lambda, s3Location, false)

    LambdaSnapshot(
      lambda = lambda,
      arn = ARN(awsCreateFunctionResponse.getFunctionArn)
    )
  }
}

class AWSCreatePublishedLambda(awsLambda: AWSLambda) {
  def apply(lambda: Lambda, s3Location: S3Location): PublishedLambda = {
    val awsCreateFunctionResponse = AWSCreateLambda.createLambda(awsLambda, lambda, s3Location, true)

    val publishedLambda = PublishedLambda(
      lambda = lambda,
      version = LambdaVersion(awsCreateFunctionResponse.getVersion.toInt),
      arn = ARN(awsCreateFunctionResponse.getFunctionArn)
    )

    publishedLambda
  }
}

object AWSCreateLambda {

  private[lambda] def createLambda(
      awsLambda: AWSLambda,
      lambda: Lambda,
      s3Location: S3Location,
      publish: Boolean
  ): CreateFunctionResult = {
    import lambda._
    import deployment._
    import runtime._

    val functionCode = new FunctionCode()
      .withS3Bucket(s3Location.bucket.name.value)
      .withS3Key(s3Location.key.value)

    val awsCreateFunctionRequest = new CreateFunctionRequest()
      .withFunctionName(name.value)
      .withHandler(handler.value)
      .withRole(roleARN.value)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withTimeout(timeout.toSeconds.toInt)
      .withMemorySize(memorySize.value)
      .withCode(functionCode)
      .withPublish(true)

    deadLetterARN.foreach { arn =>
      awsCreateFunctionRequest.withDeadLetterConfig(
        new DeadLetterConfig()
          .withTargetArn(arn.value)
      )
    }

    vpcConfig.foreach { vpc =>
      awsCreateFunctionRequest.withVpcConfig(
        new AWSVpcConfig()
          .withSecurityGroupIds(vpc.securityGroups.map(_.id).asJava)
          .withSubnetIds(vpc.subnets.map(_.id).asJava)
      )
    }

    awsLambda.createFunction(awsCreateFunctionRequest)
  }
}
