package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{ResourceNotFoundException, ListAliasesRequest => AWSListAliasesRequest}
import com.itv.aws.{ARN, AWSService}

import scala.collection.JavaConverters._

case class ListAliasesRequest(lambdaName: LambdaName)
case class ListAliasesResponse(aliases: Option[List[Alias]])


class AWSListAliases(awsLambda: AWSLambda) extends AWSService[ListAliasesRequest, ListAliasesResponse] {

  override def apply(listAliasesRequest: ListAliasesRequest): ListAliasesResponse = {
    val awsListAliasesRequest =
      new AWSListAliasesRequest().withFunctionName(listAliasesRequest.lambdaName.value)

    try {
      val listAliasesResult = awsLambda.listAliases(awsListAliasesRequest)

      val aliases = listAliasesResult.getAliases.asScala.map { c =>
        Alias(
          name = AliasName(c.getName),
          lambdaName = LambdaName(listAliasesRequest.lambdaName.value),
          lambdaVersion = LambdaVersion(c.getFunctionVersion.toInt),
          arn = ARN(c.getAliasArn)
        )
      }.toList

      ListAliasesResponse(Option(aliases))
    } catch {
      case _:ResourceNotFoundException => ListAliasesResponse(None)
    }
  }

}