package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.ResourceNotFoundException
import com.amazonaws.services.lambda.model.{ListAliasesRequest => AWSListAliasesRequest}
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._

class AWSListAliases(awsLambda: AWSLambda) {

  def apply(
      lambdaName: LambdaName
  ): Option[List[Alias]] = {
    val awsListAliasesRequest =
      new AWSListAliasesRequest()
        .withFunctionName(lambdaName.value)

    try {
      val listAliasesResult = awsLambda.listAliases(awsListAliasesRequest)

      val aliases = listAliasesResult.getAliases.asScala.map { c =>
        Alias(
          name = AliasName(c.getName),
          lambdaName = LambdaName(lambdaName.value),
          lambdaVersion = LambdaVersion(c.getFunctionVersion.toInt),
          arn = ARN(c.getAliasArn)
        )
      }.toList

      Option(aliases)
    } catch {
      case _: ResourceNotFoundException => None
    }
  }

}
