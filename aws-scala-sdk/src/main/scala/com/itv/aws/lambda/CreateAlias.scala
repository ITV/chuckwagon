package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.lambda.model.{CreateAliasRequest => AWSCreateAliasRequest}


case class CreateAliasRequest(publishedLambda: PublishedLambda, aliasName:String)
case class CreateAliasResponse(aliasedLambda: AliasedLambda)

class CreateAlias(awsLambda: AWSLambda) extends AWSService[CreateAliasRequest, CreateAliasResponse] {
  override def apply(createAliasRequest: CreateAliasRequest): CreateAliasResponse = {

    val awsCreateAliasRequest = new AWSCreateAliasRequest().
      withFunctionName(createAliasRequest.publishedLambda.lambda.name.value).
      withName(createAliasRequest.aliasName).
      withFunctionVersion(createAliasRequest.publishedLambda.version.value)

    val awsCreateAliasResponse = awsLambda.createAlias(awsCreateAliasRequest)

    CreateAliasResponse(AliasedLambda(
      publishedLambda = createAliasRequest.publishedLambda,
      alias = Alias(awsCreateAliasResponse.getName),
      arn = ARN(awsCreateAliasResponse.getAliasArn)
    ))
  }
}
