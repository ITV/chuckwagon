package com.itv.aws.ec2

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.{Filter => AWSFilter}
import com.amazonaws.services.ec2.model.{SecurityGroup => AWSSecurityGroup}

import scala.collection.JavaConverters._

class AWSFindSecurityGroups(ec2: AmazonEC2) {

  def usingFilters(vpc: Vpc, filters: List[Filter]): List[SecurityGroup] = {
    val describeSecurityGroupsRequest =
      new DescribeSecurityGroupsRequest()

    val vpcFilter =
      new AWSFilter().withName(s"vpc-id").withValues(vpc.id)

    val userFilters: List[AWSFilter] = filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }

    describeSecurityGroupsRequest.withFilters((vpcFilter :: userFilters).asJava)

    usingRequest(describeSecurityGroupsRequest)
  }

  def usingIds(vpc: Vpc, ids: List[SecurityGroupId]): List[SecurityGroup] = {
    val describeSecurityGroupsRequest =
      new DescribeSecurityGroupsRequest()

    describeSecurityGroupsRequest.withGroupIds(ids.map(_.id).asJava)

    usingRequest(describeSecurityGroupsRequest)
  }

  private def usingRequest(describeSecurityGroupsRequest: DescribeSecurityGroupsRequest): List[SecurityGroup] = {

    val awsSecurityGroups: List[AWSSecurityGroup] =
      ec2.describeSecurityGroups(describeSecurityGroupsRequest).getSecurityGroups.asScala.toList

    awsSecurityGroups.map(sg => SecurityGroup(sg.getGroupId))
  }
}
