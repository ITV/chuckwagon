package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{ListAliasesRequest => AWSListAliasesRequest}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class ListAliasesRequest(functionName: LambdaName)
case class ListAliasesResponse(aliases: List[Alias])


class ListAliases(awsLambda: AWSLambda) extends AWSService[ListAliasesRequest, ListAliasesResponse] {

  override def apply(listAliasesRequest: ListAliasesRequest): ListAliasesResponse = {
    val awsListAliasesRequest =
      new AWSListAliasesRequest().withFunctionName(listAliasesRequest.functionName.value)

    val listAliasesResult = awsLambda.listAliases(awsListAliasesRequest)

    val aliases = listAliasesResult.getAliases.asScala.map { c =>
      Alias(c.getName)
    }.toList
    ListAliasesResponse(aliases)
  }

}