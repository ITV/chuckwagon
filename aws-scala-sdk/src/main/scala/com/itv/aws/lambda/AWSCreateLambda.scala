package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.CreateFunctionRequest
import com.amazonaws.services.lambda.model.CreateFunctionResult
import com.amazonaws.services.lambda.model.FunctionCode
import com.amazonaws.services.lambda.model.{VpcConfig => AWSVpcConfig}
import com.itv.aws.s3.S3Location
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._

case class CreateLambdaRequest(lambda: Lambda, s3Location: S3Location)
case class CreatePublishedLambdaResponse(publishedLambda: PublishedLambda)
case class CreateLambdaSnapshotResponse(lambdaSnapshot: LambdaSnapshot)

class AWSCreateLambdaSnapshot(awsLambda: AWSLambda)
    extends AWSService[CreateLambdaRequest, CreateLambdaSnapshotResponse] {
  override def apply(createLambdaRequest: CreateLambdaRequest): CreateLambdaSnapshotResponse = {
    val awsCreateFunctionResponse = AWSCreateLambda.createLambda(awsLambda, createLambdaRequest, false)

    CreateLambdaSnapshotResponse(
      LambdaSnapshot(
        lambda = createLambdaRequest.lambda,
        arn = ARN(awsCreateFunctionResponse.getFunctionArn)
      )
    )
  }
}

class AWSCreatePublishedLambda(awsLambda: AWSLambda)
    extends AWSService[CreateLambdaRequest, CreatePublishedLambdaResponse] {
  override def apply(createLambdaRequest: CreateLambdaRequest): CreatePublishedLambdaResponse = {
    val awsCreateFunctionResponse = AWSCreateLambda.createLambda(awsLambda, createLambdaRequest, true)

    val publishedLambda = PublishedLambda(
      lambda = createLambdaRequest.lambda,
      version = LambdaVersion(awsCreateFunctionResponse.getVersion.toInt),
      arn = ARN(awsCreateFunctionResponse.getFunctionArn)
    )

    CreatePublishedLambdaResponse(publishedLambda)
  }
}

object AWSCreateLambda {

  private[lambda] def createLambda(
      awsLambda: AWSLambda,
      createLambdaRequest: CreateLambdaRequest,
      publish: Boolean
  ): CreateFunctionResult = {
    import createLambdaRequest.lambda._
    import deployment._
    import runtime._

    val functionCode = new FunctionCode()
      .withS3Bucket(createLambdaRequest.s3Location.bucket.name.value)
      .withS3Key(createLambdaRequest.s3Location.key.value)

    val awsCreateFunctionRequest = new CreateFunctionRequest()
      .withFunctionName(name.value)
      .withHandler(handler.value)
      .withRole(roleARN.value)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withTimeout(timeout.toSeconds.toInt)
      .withMemorySize(memorySize.value)
      .withCode(functionCode)
      .withPublish(true)

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
