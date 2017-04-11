package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{CreateAliasRequest => AWSCreateAliasRequest}
import com.itv.aws.iam.ARN

class AWSCreateAlias(awsLambda: AWSLambda) {
  def apply(
      name: AliasName,
      lambdaName: LambdaName,
      lambdaVersionToAlias: LambdaVersion
  ): Alias = {
    val lambdaNameString = lambdaName.value

    val awsCreateAliasRequest = new AWSCreateAliasRequest()
      .withFunctionName(lambdaNameString)
      .withName(name.value)
      .withFunctionVersion(
        lambdaVersionToAlias.value.toString
      )

    val awsCreateAliasResponse = awsLambda.createAlias(awsCreateAliasRequest)

    Alias(
      name = AliasName(awsCreateAliasResponse.getName),
      lambdaName = LambdaName(lambdaNameString),
      lambdaVersion = LambdaVersion(awsCreateAliasResponse.getFunctionVersion.toInt),
      arn = ARN(awsCreateAliasResponse.getAliasArn)
    )
  }
}
