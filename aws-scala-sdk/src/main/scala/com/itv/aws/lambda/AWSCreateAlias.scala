package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.lambda.model.{CreateAliasRequest => AWSCreateAliasRequest}

case class CreateAliasRequest(name: AliasName, lambdaName: LambdaName, lambdaVersionToAlias: LambdaVersion)
case class CreateAliasResponse(aliasedLambda: Alias)

class AWSCreateAlias(awsLambda: AWSLambda) extends AWSService[CreateAliasRequest, CreateAliasResponse] {
  override def apply(
      createAliasRequest: CreateAliasRequest
  ): CreateAliasResponse = {
    val lambdaNameString = createAliasRequest.lambdaName.value

    val awsCreateAliasRequest = new AWSCreateAliasRequest()
      .withFunctionName(lambdaNameString)
      .withName(createAliasRequest.name.value)
      .withFunctionVersion(
        createAliasRequest.lambdaVersionToAlias.value.toString
      )

    val awsCreateAliasResponse = awsLambda.createAlias(awsCreateAliasRequest)

    CreateAliasResponse(
      Alias(
        name = AliasName(awsCreateAliasResponse.getName),
        lambdaName = LambdaName(lambdaNameString),
        lambdaVersion = LambdaVersion(awsCreateAliasResponse.getFunctionVersion.toInt),
        arn = ARN(awsCreateAliasResponse.getAliasArn)
      )
    )
  }
}
