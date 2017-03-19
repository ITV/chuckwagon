package com.itv.aws.ec2

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.{Filter => AWSFilter}
import com.amazonaws.services.ec2.model.{SecurityGroup => AWSSecurityGroup}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class FindSecurityGroupsRequest(vpc: VPC, filters: List[Filter])

case class FindSecurityGroupsResponse(securityGroups: List[SecurityGroup])

class AWSFindSecurityGroups(ec2: AmazonEC2)
    extends AWSService[FindSecurityGroupsRequest, FindSecurityGroupsResponse] {
  override def apply(findSecurityGroupsRequest: FindSecurityGroupsRequest): FindSecurityGroupsResponse = {

    val userFilters: List[AWSFilter] = findSecurityGroupsRequest.filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }

    val vpcFilter =
      new AWSFilter().withName(s"vpc-id").withValues(findSecurityGroupsRequest.vpc.id)

    val describeSecurityGroupsRequest =
      new DescribeSecurityGroupsRequest()
        .withFilters((vpcFilter :: userFilters).asJava)

    val awsSecurityGroups: List[AWSSecurityGroup] =
      ec2.describeSecurityGroups(describeSecurityGroupsRequest).getSecurityGroups.asScala.toList

    val securityGroups = awsSecurityGroups.map(sg => SecurityGroup(sg.getGroupId))

    FindSecurityGroupsResponse(securityGroups)
  }
}
