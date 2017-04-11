package com.itv.chuckwagon.deploy

import cats.free.Free.liftF
import com.itv.aws.ec2._
import com.itv.aws.lambda.VpcConfig

sealed trait VpcConfigLookup

case class VpcConfigUsingFiltersLookup(
    vpcLookupFilters: List[Filter],
    subnetsLookupFilters: List[Filter],
    securityGroupsLookupFilters: List[Filter]
) extends VpcConfigLookup

case class VpcConfigUsingIdsLookup(
    id: VpcId,
    subnetIds: List[SubnetId],
    securityGroupIds: List[SecurityGroupId]
) extends VpcConfigLookup

trait VpcCommands {

  def findSecurityGroupsUsingFilters(vpc: Vpc, filters: List[Filter]): DeployLambda[List[SecurityGroup]] =
    liftF[DeployLambdaA, List[SecurityGroup]](
      FindSecurityGroupsUsingFilters(vpc, filters)
    )
  def findSecurityGroupsUsingIds(vpc: Vpc, ids: List[SecurityGroupId]): DeployLambda[List[SecurityGroup]] =
    liftF[DeployLambdaA, List[SecurityGroup]](
      FindSecurityGroupsUsingIds(vpc, ids)
    )
  def findSubnetsUsingFilters(vpc: Vpc, filters: List[Filter]): DeployLambda[List[Subnet]] =
    liftF[DeployLambdaA, List[Subnet]](
      FindSubnetsUsingFilters(vpc, filters)
    )
  def findSubnetsUsingIds(vpc: Vpc, ids: List[SubnetId]): DeployLambda[List[Subnet]] =
    liftF[DeployLambdaA, List[Subnet]](
      FindSubnetsUsingIds(vpc, ids)
    )
  def findVPCUsingFilters(filters: List[Filter]): DeployLambda[Vpc] =
    liftF[DeployLambdaA, Vpc](
      FindVpcUsingFilters(filters)
    )
  def findVPCUsingId(vpcId: VpcId): DeployLambda[Vpc] =
    liftF[DeployLambdaA, Vpc](
      FindVpcUsingId(vpcId)
    )

  def getVpcConfig(vpcConfigDeclaration: VpcConfigLookup): DeployLambda[Option[VpcConfig]] =
    vpcConfigDeclaration match {
      case f: VpcConfigUsingFiltersLookup => getVpcConfigUsingFilters(f)
      case f: VpcConfigUsingIdsLookup     => getVpcConfigUsingIds(f)
    }

  private def getVpcConfigUsingFilters(
      vpcLookup: VpcConfigUsingFiltersLookup
  ): DeployLambda[Option[VpcConfig]] =
    for {
      vpc     <- findVPCUsingFilters(vpcLookup.vpcLookupFilters)
      subnets <- findSubnetsUsingFilters(vpc, vpcLookup.subnetsLookupFilters)
      securityGroups <- findSecurityGroupsUsingFilters(
        vpc,
        vpcLookup.securityGroupsLookupFilters
      )
    } yield {
      Some(VpcConfig(vpc, subnets, securityGroups))
    }

  private def getVpcConfigUsingIds(vpcLookup: VpcConfigUsingIdsLookup): DeployLambda[Option[VpcConfig]] =
    for {
      vpc     <- findVPCUsingId(vpcLookup.id)
      subnets <- findSubnetsUsingIds(vpc, vpcLookup.subnetIds)
      securityGroups <- findSecurityGroupsUsingIds(
        vpc,
        vpcLookup.securityGroupIds
      )
    } yield {
      Some(VpcConfig(vpc, subnets, securityGroups))
    }
}

object VpcCommands extends VpcCommands
