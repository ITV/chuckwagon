package com.itv.aws.ec2

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.{DescribeVpcsRequest, Filter => AWSFilter}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class FindVPCRequest(filters: List[Filter])

case class FindVPCResponse(vpc: VPC)

class AWSFindVPC(ec2: AmazonEC2) extends AWSService[FindVPCRequest, FindVPCResponse] {
  override def apply(listRolesRequest: FindVPCRequest): FindVPCResponse = {

    val filters: List[AWSFilter] = listRolesRequest.filters.map { tag =>
      new AWSFilter().withName(s"${tag.key}").withValues(tag.value)
    }

    val describeVpcsRequest = new DescribeVpcsRequest().withFilters(filters.asJava)

    val vpcs: List[VPC] = ec2
      .describeVpcs(describeVpcsRequest)
      .getVpcs
      .asScala
      .map { vpc =>
        VPC(id = vpc.getVpcId)
      }
      .toList

    vpcs.headOption match {
      case Some(vpc) if vpcs.size == 1 => FindVPCResponse(vpc)
      case _ =>
        throw new Exception(s"AWSFindVPC expected tags to match one VPC but matched ${vpcs.size} with ids ${vpcs}.")
    }
  }
}
