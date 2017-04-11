package com.itv.aws.ec2

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest
import com.amazonaws.services.ec2.model.{Filter => AWSFilter}
import com.amazonaws.services.ec2.model.{Subnet => AWSSubnet}

import scala.collection.JavaConverters._

class AWSFindSubnets(ec2: AmazonEC2) {

  def usingFilters(vpc: Vpc, filters: List[Filter]): List[Subnet] = {
    val describeSubnetsRequest = new DescribeSubnetsRequest()

    val vpcFilter = new AWSFilter().withName(s"vpc-id").withValues(vpc.id)

    val userFilters: List[AWSFilter] = filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }
    describeSubnetsRequest.withFilters((vpcFilter :: userFilters).asJava)

    usingRequest(describeSubnetsRequest)
  }
  def usingIds(vpc: Vpc, ids: List[SubnetId]): List[Subnet] = {
    val describeSubnetsRequest = new DescribeSubnetsRequest()

    describeSubnetsRequest.withSubnetIds(ids.map(_.id).asJava)

    usingRequest(describeSubnetsRequest)
  }

  private def usingRequest(describeSubnetsRequest: DescribeSubnetsRequest): List[Subnet] = {

    val awsSubnets: List[AWSSubnet] =
      ec2.describeSubnets(describeSubnetsRequest).getSubnets.asScala.toList

    awsSubnets.map(sn => Subnet(sn.getSubnetId))
  }
}
