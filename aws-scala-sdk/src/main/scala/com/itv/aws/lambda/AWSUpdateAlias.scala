package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.lambda.model.{UpdateAliasRequest => AWSUpdateAliasRequest}

case class UpdateAliasRequest(alias: Alias, lambdaVersionToAlias: LambdaVersion)
case class UpdateAliasResponse(alias: Alias)

class AWSUpdateAlias(awsLambda: AWSLambda) extends AWSService[UpdateAliasRequest, UpdateAliasResponse] {
  override def apply(
      updateAliasRequest: UpdateAliasRequest
  ): UpdateAliasResponse = {

    val lambdaNameString = updateAliasRequest.alias.lambdaName.value

    val awsUpdateAliasRequest = new AWSUpdateAliasRequest()
      .withFunctionName(lambdaNameString)
      .withName(updateAliasRequest.alias.name.value)
      .withFunctionVersion(
        updateAliasRequest.lambdaVersionToAlias.value.toString
      )

    val awsUpdateAliasResponse = awsLambda.updateAlias(awsUpdateAliasRequest)

    UpdateAliasResponse(
      Alias(
        name = AliasName(awsUpdateAliasRequest.getName),
        lambdaName = LambdaName(lambdaNameString),
        lambdaVersion = LambdaVersion(awsUpdateAliasResponse.getFunctionVersion.toInt),
        arn = ARN(awsUpdateAliasResponse.getAliasArn)
      )
    )
  }
}
