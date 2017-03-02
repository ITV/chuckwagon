package com.itv.aws.ec2

import com.amazonaws.services.ec2.model.{DescribeSubnetsRequest, Filter => AWSFilter, Subnet => AWSSubnet}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class FindSubnetsRequest(vpc: VPC, filters: List[Filter])

case class FindSubnetsResponse(subnets: List[Subnet])

object AWSFindSubnets extends AWSService[FindSubnetsRequest, FindSubnetsResponse] {

  override def apply(findSubnetsRequest: FindSubnetsRequest): FindSubnetsResponse = {

    val userFilters: List[AWSFilter] = findSubnetsRequest.filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }

    val vpcFilter = new AWSFilter().withName(s"vpc-id").withValues(findSubnetsRequest.vpc.id)

    val describeSubnetsRequest =
      new DescribeSubnetsRequest().withFilters((vpcFilter :: userFilters).asJava)

    val awsSubnets: List[AWSSubnet] =
      ec2.describeSubnets(describeSubnetsRequest).getSubnets.asScala.toList

    val subnets = awsSubnets.map(sn => Subnet(sn.getSubnetId))

    FindSubnetsResponse(subnets)
  }
}
