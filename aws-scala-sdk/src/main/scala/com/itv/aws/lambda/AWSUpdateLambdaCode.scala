package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.itv.aws.s3.S3Location
import com.itv.aws.{ARN, AWSService}

case class UpdateLambdaCodeRequest(lambda: Lambda, s3Location: S3Location)
case class UpdateLambdaCodeResponse(publishedLambda: PublishedLambda)

class AWSUpdateLambdaCode(awsLambda: AWSLambda)
    extends AWSService[UpdateLambdaCodeRequest, UpdateLambdaCodeResponse] {
  override def apply(
    updateLambdaRequest: UpdateLambdaCodeRequest
  ): UpdateLambdaCodeResponse = {
    import updateLambdaRequest._

    val awsUpdateFunctionRequest = new UpdateFunctionCodeRequest()
      .withFunctionName(lambda.deployment.name.value)
      .withS3Bucket(s3Location.bucket.name.value)
      .withS3Key(s3Location.key.value)
      .withPublish(true)

    val awsUpdateFunctionResponse =
      awsLambda.updateFunctionCode(awsUpdateFunctionRequest)

    val publishedLambda = PublishedLambda(
      lambda = updateLambdaRequest.lambda,
      version = LambdaVersion(awsUpdateFunctionResponse.getVersion.toInt),
      arn = ARN(awsUpdateFunctionResponse.getFunctionArn)
    )

    UpdateLambdaCodeResponse(publishedLambda)
  }
}
