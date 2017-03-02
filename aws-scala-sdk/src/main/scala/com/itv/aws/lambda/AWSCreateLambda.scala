package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{CreateFunctionRequest, FunctionCode, VpcConfig => AWSVpcConfig}
import com.itv.aws.s3.S3Location
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._

case class CreateLambdaRequest(lambda: Lambda, s3Location: S3Location)
case class CreateLambdaResponse(publishedLambda: PublishedLambda)

class AWSCreateLambda(awsLambda: AWSLambda) extends AWSService[CreateLambdaRequest, CreateLambdaResponse] {

  override def apply(
      createLambdaRequest: CreateLambdaRequest
  ): CreateLambdaResponse = {
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

    val awsCreateFunctionResponse =
      awsLambda.createFunction(awsCreateFunctionRequest)

    val publishedLambda = PublishedLambda(
      lambda = createLambdaRequest.lambda,
      version = LambdaVersion(awsCreateFunctionResponse.getVersion.toInt),
      arn = ARN(awsCreateFunctionResponse.getFunctionArn)
    )

    CreateLambdaResponse(publishedLambda)
  }
}
