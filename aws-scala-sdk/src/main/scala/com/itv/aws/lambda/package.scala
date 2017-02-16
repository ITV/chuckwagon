package com.itv.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.{AWSLambda, AWSLambdaClientBuilder}

import scala.concurrent.duration.FiniteDuration

package object lambda {

  case class LambdaName(value: String) extends AnyVal
  case class LambdaVersion(value: Int) extends AnyVal
  case class LambdaHandler(value: String) extends AnyVal
  case class MemorySize(value: Int) extends AnyVal

  case class AliasName(value: String) extends AnyVal
  case class Alias(name: AliasName, lambdaVersion: LambdaVersion)

  case class LambdaConfiguration(
                                  roleARN: ARN,
                                  handler: LambdaHandler,
                                  timeout: FiniteDuration,
                                  memorySize: MemorySize
                                  )
  case class Lambda(name: LambdaName,configuration: LambdaConfiguration)
  case class PublishedLambda(lambda: Lambda,
                             version: LambdaVersion,
                             arn: ARN
                            )
  case class AliasedLambda(publishedLambda: PublishedLambda,
                           alias: Alias,
                           arn: ARN
                          )

  def awsLambda(region:Regions): AWSLambda =
    configuredClientForRegion(AWSLambdaClientBuilder.standard())(region)

}
