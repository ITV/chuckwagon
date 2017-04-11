package com.itv.aws.ec2

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeVpcsRequest
import com.amazonaws.services.ec2.model.{Filter => AWSFilter}

import scala.collection.JavaConverters._

class AWSFindVpc(ec2: AmazonEC2) {
  def usingFilters(filters: List[Filter]): Vpc = {
    val describeVpcsRequest = new DescribeVpcsRequest()

    val awsFilters: List[AWSFilter] = filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }
    describeVpcsRequest.withFilters(awsFilters.asJava)

    usingRequest(describeVpcsRequest)
  }

  def usingId(id: VpcId): Vpc = {
    val describeVpcsRequest = new DescribeVpcsRequest()
    describeVpcsRequest.withVpcIds(id.value)
    usingRequest(describeVpcsRequest)
  }

  private def usingRequest(describeVpcsRequest: DescribeVpcsRequest): Vpc = {
    val vpcs: List[Vpc] = ec2
      .describeVpcs(describeVpcsRequest)
      .getVpcs
      .asScala
      .map { vpc =>
        Vpc(id = vpc.getVpcId)
      }
      .toList

    vpcs.headOption match {
      case Some(vpc) if vpcs.size == 1 => vpc
      case _ =>
        throw new Exception(
          s"AWSFindVPC expected tags to match one VPC but matched ${vpcs.size} with ids ${vpcs}."
        )
    }
  }
}
