package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.DeleteFunctionRequest
import com.itv.aws.AWSService

case class DeleteLambdaVersionRequest(publishedLambda: PublishedLambda)
case class DeleteLambdaVersionResponse(deletedVersion: LambdaVersion)

class AWSDeleteLambdaVersion(awsLambda: AWSLambda)
    extends AWSService[DeleteLambdaVersionRequest, DeleteLambdaVersionResponse] {
  override def apply(
      deleteLambdaRequest: DeleteLambdaVersionRequest
  ): DeleteLambdaVersionResponse = {
    import deleteLambdaRequest.publishedLambda._

    val awsDeleteFunctionRequest = new DeleteFunctionRequest()
      .withFunctionName(lambda.deployment.name.value)
      .withQualifier(version.value.toString)

    val _ = awsLambda.deleteFunction(awsDeleteFunctionRequest)

    DeleteLambdaVersionResponse(version)
  }
}
