package com.itv.aws

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder

package object ec2 {
  def ec2: AwsClientBuilder[AmazonEC2] = configuredClientForRegion(AmazonEC2ClientBuilder.standard())
}

package ec2 {
  case class VpcId(value: String) extends AnyVal
  case class Vpc(id: String)      extends AnyVal
  case class Filter(key: String, value: String)

  case class SecurityGroupId(id: String) extends AnyVal
  case class SecurityGroup(id: String)   extends AnyVal

  case class SubnetId(id: String) extends AnyVal
  case class Subnet(id: String)   extends AnyVal
}
