package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.ListVersionsByFunctionRequest
import com.amazonaws.services.lambda.model.ResourceNotFoundException
import com.itv.aws.iam.ARN

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

class AWSListPublishedLambdasWithName(awsLambda: AWSLambda) {

  def apply(
      lambdaName: LambdaName
  ): Option[List[PublishedLambda]] = {

    val listVersionsByFunctionRequest =
      new ListVersionsByFunctionRequest()
        .withFunctionName(lambdaName.value)

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
      Option(fcs)
    } catch {
      case _: ResourceNotFoundException =>
        None
    }
  }
}
