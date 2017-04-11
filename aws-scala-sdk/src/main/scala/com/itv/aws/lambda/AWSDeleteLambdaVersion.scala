package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.DeleteFunctionRequest

class AWSDeleteLambdaVersion(awsLambda: AWSLambda) {
  def apply(
      publishedLambda: PublishedLambda
  ): LambdaVersion = {

    val awsDeleteFunctionRequest = new DeleteFunctionRequest()
      .withFunctionName(publishedLambda.lambda.deployment.name.value)
      .withQualifier(publishedLambda.version.value.toString)

    val _ = awsLambda.deleteFunction(awsDeleteFunctionRequest)

    publishedLambda.version
  }
}
