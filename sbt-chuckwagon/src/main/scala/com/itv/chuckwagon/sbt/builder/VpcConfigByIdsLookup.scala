package com.itv.chuckwagon.sbt.builder

import com.itv.aws.ec2.SecurityGroupId
import com.itv.aws.ec2.SubnetId
import com.itv.aws.ec2.VpcId
import com.itv.chuckwagon.deploy.VpcConfigUsingIdsLookup

import scala.language.implicitConversions

object VpcConfigUsingIdsLookupBuilder {
  implicit def enableVpcConfigDeclarationBuilder(
      builder: VpcConfigUsingIdsLookupBuilder[DEFINED, DEFINED, DEFINED]
  ): VpcConfigUsingIdsLookup =
    VpcConfigUsingIdsLookup(
      builder.id.get,
      builder.subnetIds.get,
      builder.securityGroupIds.get
    )

  abstract class UNDEFINED_VpcId
  abstract class UNDEFINED_SubnetIds
  abstract class UNDEFINED_SecurityGroupIds

  def apply() =
    new VpcConfigUsingIdsLookupBuilder[
      UNDEFINED_VpcId,
      UNDEFINED_SubnetIds,
      UNDEFINED_SecurityGroupIds
    ](None, None, None)
}

class VpcConfigUsingIdsLookupBuilder[B_VPC_ID, B_SUBNET_IDS, B_SECURITYGROUP_IDS](
    val id: Option[VpcId],
    val subnetIds: Option[List[SubnetId]],
    val securityGroupIds: Option[List[SecurityGroupId]]
) {
  def withVpcId(vpcId: String) =
    new VpcConfigUsingIdsLookupBuilder[DEFINED, B_SUBNET_IDS, B_SECURITYGROUP_IDS](
      Option(VpcId(vpcId)),
      subnetIds,
      securityGroupIds
    )

  def withSubnetIds(subnetIds: String*) =
    new VpcConfigUsingIdsLookupBuilder[B_VPC_ID, DEFINED, B_SECURITYGROUP_IDS](
      id,
      Option(subnetIds.toList.map(SubnetId)),
      securityGroupIds
    )

  def withSecurityGroupIds(securityGroupIds: String*) =
    new VpcConfigUsingIdsLookupBuilder[B_VPC_ID, B_SUBNET_IDS, DEFINED](
      id,
      subnetIds,
      Option(securityGroupIds.toList.map(SecurityGroupId))
    )
}
