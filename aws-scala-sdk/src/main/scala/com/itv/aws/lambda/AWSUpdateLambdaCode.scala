package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult
import com.itv.aws.s3.S3Location
import com.itv.aws.iam.ARN

class AWSUpdateCodeForLambdaSnapshot(awsLambda: AWSLambda) {
  def apply(lambda: Lambda, s3Location: S3Location): LambdaSnapshot = {
    val awsUpdateFunctionResponse = AWSUpdateLambdaCode.updateLambda(awsLambda, lambda, s3Location, false)

    val lambdaSnapshot = LambdaSnapshot(
      lambda = lambda,
      arn = ARN(awsUpdateFunctionResponse.getFunctionArn)
    )

    lambdaSnapshot
  }
}

class AWSUpdateCodeAndPublishLambda(awsLambda: AWSLambda) {
  def apply(lambda: Lambda, s3Location: S3Location): PublishedLambda = {
    val awsUpdateFunctionResponse = AWSUpdateLambdaCode.updateLambda(awsLambda, lambda, s3Location, true)

    val publishedLambda = PublishedLambda(
      lambda = lambda,
      version = LambdaVersion(awsUpdateFunctionResponse.getVersion.toInt),
      arn = ARN(awsUpdateFunctionResponse.getFunctionArn)
    )

    publishedLambda
  }
}

object AWSUpdateLambdaCode {
  private[lambda] def updateLambda(
      awsLambda: AWSLambda,
      lambda: Lambda,
      s3Location: S3Location,
      publish: Boolean
  ): UpdateFunctionCodeResult = {

    val awsUpdateFunctionRequest = new UpdateFunctionCodeRequest()
      .withFunctionName(lambda.deployment.name.value)
      .withS3Bucket(s3Location.bucket.name.value)
      .withS3Key(s3Location.key.value)
      .withPublish(publish)

    awsLambda.updateFunctionCode(awsUpdateFunctionRequest)
  }
}
