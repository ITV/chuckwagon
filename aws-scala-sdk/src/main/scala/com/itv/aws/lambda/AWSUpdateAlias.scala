package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{UpdateAliasRequest => AWSUpdateAliasRequest}
import com.itv.aws.iam.ARN

class AWSUpdateAlias(awsLambda: AWSLambda) {
  def apply(
      alias: Alias,
      lambdaVersionToAlias: LambdaVersion
  ): Alias = {

    val lambdaNameString = alias.lambdaName.value

    val awsUpdateAliasRequest = new AWSUpdateAliasRequest()
      .withFunctionName(lambdaNameString)
      .withName(alias.name.value)
      .withFunctionVersion(
        lambdaVersionToAlias.value.toString
      )

    val awsUpdateAliasResponse = awsLambda.updateAlias(awsUpdateAliasRequest)

    Alias(
      name = AliasName(awsUpdateAliasRequest.getName),
      lambdaName = LambdaName(lambdaNameString),
      lambdaVersion = LambdaVersion(awsUpdateAliasResponse.getFunctionVersion.toInt),
      arn = ARN(awsUpdateAliasResponse.getAliasArn)
    )
  }
}
