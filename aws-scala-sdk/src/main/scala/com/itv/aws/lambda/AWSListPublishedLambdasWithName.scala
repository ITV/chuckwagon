package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.ListVersionsByFunctionRequest
import com.itv.aws.{ARN, AWSService}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

case class ListPublishedLambdasWithNameRequest(lambdaName: LambdaName)
case class ListPublishedLambdasWithNameResponse(publishedLambdas: List[PublishedLambda])


class AWSListPublishedLambdasWithName(awsLambda: AWSLambda) extends AWSService[ListPublishedLambdasWithNameRequest, ListPublishedLambdasWithNameResponse] {

  override def apply(listFunctionsWithNameRequest: ListPublishedLambdasWithNameRequest): ListPublishedLambdasWithNameResponse = {

    val listVersionsByFunctionRequest =
      new ListVersionsByFunctionRequest()
        .withFunctionName(listFunctionsWithNameRequest.lambdaName.value)

    val listFunctionsResult = awsLambda.listVersionsByFunction(listVersionsByFunctionRequest)

// TODO filter isint
    val fcs = listFunctionsResult.getVersions.asScala.map { fc =>
      PublishedLambda(
        lambda = Lambda(
          name = LambdaName(fc.getFunctionName),
          configuration = LambdaConfiguration(
            roleARN = ARN(fc.getRole),
            handler = LambdaHandler(fc.getHandler),
            timeout = fc.getTimeout.toInt.seconds,
            memorySize = MemorySize(fc.getMemorySize)
          )
        ),
        arn = ARN(fc.getFunctionArn),
        version = LambdaVersion(fc.getVersion.toInt)
      )
    }.toList
    ListPublishedLambdasWithNameResponse(fcs)
  }
}
