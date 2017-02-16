package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{ListAliasesRequest => AWSListAliasesRequest}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class ListAliasesRequest(lambdaName: LambdaName)
case class ListAliasesResponse(aliases: List[Alias])


class AWSListAliases(awsLambda: AWSLambda) extends AWSService[ListAliasesRequest, ListAliasesResponse] {

  override def apply(listAliasesRequest: ListAliasesRequest): ListAliasesResponse = {
    val awsListAliasesRequest =
      new AWSListAliasesRequest().withFunctionName(listAliasesRequest.lambdaName.value)

    val listAliasesResult = awsLambda.listAliases(awsListAliasesRequest)

    val aliases = listAliasesResult.getAliases.asScala.map { c =>
      Alias(
        name = AliasName(c.getName),
        lambdaVersion = LambdaVersion(c.getFunctionVersion.toInt)
      )
    }.toList
    ListAliasesResponse(aliases)
  }

}