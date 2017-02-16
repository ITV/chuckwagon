package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.lambda.model.{UpdateAliasRequest => AWSUpdateAliasRequest}


case class UpdateAliasRequest(publishedLambda: PublishedLambda, aliasName:AliasName)
case class UpdateAliasResponse(aliasedLambda: AliasedLambda)

class AWSUpdateAlias(awsLambda: AWSLambda) extends AWSService[UpdateAliasRequest, UpdateAliasResponse] {
  override def apply(updateAliasRequest: UpdateAliasRequest): UpdateAliasResponse = {

    val awsUpdateAliasRequest = new AWSUpdateAliasRequest().
      withFunctionName(updateAliasRequest.publishedLambda.lambda.name.value).
      withName(updateAliasRequest.aliasName.value).
      withFunctionVersion(updateAliasRequest.publishedLambda.version.value.toString)

    val awsUpdateAliasResponse = awsLambda.updateAlias(awsUpdateAliasRequest)

    UpdateAliasResponse(AliasedLambda(
      publishedLambda = updateAliasRequest.publishedLambda,
      alias = Alias(
        name = AliasName(awsUpdateAliasRequest.getName),
        lambdaVersion = updateAliasRequest.publishedLambda.version
      ),
      arn = ARN(awsUpdateAliasResponse.getAliasArn)
    ))
  }
}
