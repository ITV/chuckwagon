package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.lambda.model.{CreateAliasRequest => AWSCreateAliasRequest}


case class CreateAliasRequest(publishedLambda: PublishedLambda, aliasName:AliasName)
case class CreateAliasResponse(aliasedLambda: AliasedLambda)

class AWSCreateAlias(awsLambda: AWSLambda) extends AWSService[CreateAliasRequest, CreateAliasResponse] {
  override def apply(createAliasRequest: CreateAliasRequest): CreateAliasResponse = {

    val awsCreateAliasRequest = new AWSCreateAliasRequest().
      withFunctionName(createAliasRequest.publishedLambda.lambda.name.value).
      withName(createAliasRequest.aliasName.value).
      withFunctionVersion(createAliasRequest.publishedLambda.version.value.toString)

    val awsCreateAliasResponse = awsLambda.createAlias(awsCreateAliasRequest)

    CreateAliasResponse(AliasedLambda(
      publishedLambda = createAliasRequest.publishedLambda,
      alias = Alias(
        name = AliasName(awsCreateAliasResponse.getName),
        lambdaVersion = LambdaVersion(awsCreateAliasResponse.getFunctionVersion.toInt)
      ),
      arn = ARN(awsCreateAliasResponse.getAliasArn)
    ))
  }
}
