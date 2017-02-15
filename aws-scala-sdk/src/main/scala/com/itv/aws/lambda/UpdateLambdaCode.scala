package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.itv.aws.s3.S3Location
import com.itv.aws.AWSService


case class UpdateLambdaCodeRequest(
                                    lambda: Lambda,
                                    s3Location: S3Location
                                  )
case class UpdateLambdaCodeResponse()


class UpdateLambdaCode(awsLambda: AWSLambda) extends AWSService[UpdateLambdaCodeRequest, UpdateLambdaCodeResponse] {
  override def apply(updateLambdaRequest: UpdateLambdaCodeRequest): UpdateLambdaCodeResponse = {
    import updateLambdaRequest._

    val awsUpdateFunctionRequest = new UpdateFunctionCodeRequest().
      withFunctionName(lambda.name.value).
      withS3Bucket(s3Location.bucket.name).
      withS3Key(s3Location.key.value)

    val _ = awsLambda.updateFunctionCode(awsUpdateFunctionRequest)

    UpdateLambdaCodeResponse()
  }
}
