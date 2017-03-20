package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult
import com.itv.aws.s3.S3Location
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN

case class UpdateLambdaCodeRequest(lambda: Lambda, s3Location: S3Location)
case class UpdatePublishedLambdaCodeResponse(publishedLambda: PublishedLambda)
case class UpdateLambdaSnapshotCodeResponse(lambdaSnapshot: LambdaSnapshot)

class AWSUpdateCodeForLambdaSnapshot(awsLambda: AWSLambda)
    extends AWSService[UpdateLambdaCodeRequest, UpdateLambdaSnapshotCodeResponse] {
  override def apply(updateLambdaRequest: UpdateLambdaCodeRequest): UpdateLambdaSnapshotCodeResponse = {
    val awsUpdateFunctionResponse = AWSUpdateLambdaCode.updateLambda(awsLambda, updateLambdaRequest, false)

    val lambdaSnapshot = LambdaSnapshot(
      lambda = updateLambdaRequest.lambda,
      arn = ARN(awsUpdateFunctionResponse.getFunctionArn)
    )

    UpdateLambdaSnapshotCodeResponse(lambdaSnapshot)
  }
}

class AWSUpdateCodeAndPublishLambda(awsLambda: AWSLambda)
    extends AWSService[UpdateLambdaCodeRequest, UpdatePublishedLambdaCodeResponse] {
  override def apply(updateLambdaRequest: UpdateLambdaCodeRequest): UpdatePublishedLambdaCodeResponse = {
    val awsUpdateFunctionResponse = AWSUpdateLambdaCode.updateLambda(awsLambda, updateLambdaRequest, true)

    val publishedLambda = PublishedLambda(
      lambda = updateLambdaRequest.lambda,
      version = LambdaVersion(awsUpdateFunctionResponse.getVersion.toInt),
      arn = ARN(awsUpdateFunctionResponse.getFunctionArn)
    )

    UpdatePublishedLambdaCodeResponse(publishedLambda)
  }
}

object AWSUpdateLambdaCode {
  private[lambda] def updateLambda(
      awsLambda: AWSLambda,
      updateLambdaRequest: UpdateLambdaCodeRequest,
      publish: Boolean
  ): UpdateFunctionCodeResult = {
    import updateLambdaRequest._

    val awsUpdateFunctionRequest = new UpdateFunctionCodeRequest()
      .withFunctionName(lambda.deployment.name.value)
      .withS3Bucket(s3Location.bucket.name.value)
      .withS3Key(s3Location.key.value)
      .withPublish(publish)

    awsLambda.updateFunctionCode(awsUpdateFunctionRequest)
  }
}
