package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{ListVersionsByFunctionRequest, ResourceNotFoundException}
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

case class ListPublishedLambdasWithNameRequest(lambdaName: LambdaName)
case class ListPublishedLambdasWithNameResponse(
    publishedLambdas: Option[List[PublishedLambda]]
)

class AWSListPublishedLambdasWithName(awsLambda: AWSLambda)
    extends AWSService[
      ListPublishedLambdasWithNameRequest,
      ListPublishedLambdasWithNameResponse
    ] {

  override def apply(
      listFunctionsWithNameRequest: ListPublishedLambdasWithNameRequest
  ): ListPublishedLambdasWithNameResponse = {

    val listVersionsByFunctionRequest =
      new ListVersionsByFunctionRequest()
        .withFunctionName(listFunctionsWithNameRequest.lambdaName.value)

    try {
      val listFunctionsResult =
        awsLambda.listVersionsByFunction(listVersionsByFunctionRequest)
      val publishedVersions = listFunctionsResult.getVersions.asScala
        .filter(fc => Try(fc.getVersion.toInt).isSuccess)

      val fcs = publishedVersions.map { fc =>
        PublishedLambda(
          lambda = Lambda(
            deployment = LambdaDeploymentConfiguration(
              name = LambdaName(fc.getFunctionName),
              roleARN = ARN(fc.getRole),
              vpcConfig = None // TODO actually set this...
            ),
            runtime = LambdaRuntimeConfiguration(
              handler = LambdaHandler(fc.getHandler),
              timeout = fc.getTimeout.toDouble.seconds,
              memorySize = MemorySize(fc.getMemorySize)
            )
          ),
          arn = ARN(fc.getFunctionArn),
          version = LambdaVersion(fc.getVersion.toInt)
        )
      }.toList
      ListPublishedLambdasWithNameResponse(Option(fcs))
    } catch {
      case _: ResourceNotFoundException =>
        ListPublishedLambdasWithNameResponse(None)
    }
  }
}
