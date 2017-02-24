package com.itv.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.{AWSLambda, AWSLambdaClientBuilder}
import com.itv.aws.ec2.{SecurityGroup, Subnet, Filter, VPC}

import scala.concurrent.duration.FiniteDuration

package object lambda {

  case class LambdaName(value: String) extends AnyVal
  case class LambdaVersion(value: Int) extends AnyVal
  case class LambdaHandler(value: String) extends AnyVal
  case class MemorySize(value: Int) extends AnyVal

  case class AliasName(value: String) extends AnyVal
  case class Alias(name: AliasName,
                   lambdaName: LambdaName,
                   lambdaVersion: LambdaVersion,
                   arn: ARN)

  case class LambdaConfiguration(roleARN: ARN,
                                 handler: LambdaHandler,
                                 timeout: FiniteDuration,
                                 memorySize: MemorySize,
                                 vpcConfig: Option[VpcConfig] = None)
  case class Lambda(name: LambdaName, configuration: LambdaConfiguration) {
    def withVpcConfig(vpcConfig: VpcConfig):Lambda = {
      val updatedConfiguration = configuration.copy(vpcConfig = Option(vpcConfig))
      this.copy(configuration = updatedConfiguration)
    }
  }
  case class PublishedLambda(lambda: Lambda, version: LambdaVersion, arn: ARN)

  case class VpcConfigDeclaration(
                                   vpcLookupFilters: List[Filter],
                                   subnetsLookupFilters: List[Filter],
                                   securityGroupsLookupFilters: List[Filter]
                                 )

  case class VpcConfig(vpc: VPC, subnets:List[Subnet], securityGroups: List[SecurityGroup])

  def awsLambda(region: Regions): AWSLambda =
    configuredClientForRegion(AWSLambdaClientBuilder.standard())(region)

}
