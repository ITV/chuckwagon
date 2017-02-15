package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{CreateFunctionRequest, FunctionCode}
import com.itv.aws.s3.S3Location
import com.itv.aws.AWSService


case class CreateLambdaRequest(
                                lambda: Lambda,
                                s3Location: S3Location
                              )
case class CreateLambdaResponse()


class CreateLambda(awsLambda: AWSLambda) extends AWSService[CreateLambdaRequest, CreateLambdaResponse] {
  override def apply(createLambdaRequest: CreateLambdaRequest): CreateLambdaResponse = {
    import createLambdaRequest.lambda._

    val functionCode = new FunctionCode().
      withS3Bucket(createLambdaRequest.s3Location.bucket.name).
      withS3Key(createLambdaRequest.s3Location.key.value)

    val awsCreateFunctionRequest = new CreateFunctionRequest().
      withFunctionName(name.value).
      withHandler(configuration.handler.value).
      withRole(configuration.roleARN.value).
      withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8).
      withTimeout(configuration.timeout.toSeconds.toInt).
      withMemorySize(configuration.memorySize.value).
      withCode(functionCode)

    val _ = awsLambda.createFunction(awsCreateFunctionRequest)

    CreateLambdaResponse()
  }
}
