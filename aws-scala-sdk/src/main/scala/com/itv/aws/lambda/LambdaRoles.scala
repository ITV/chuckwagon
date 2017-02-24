package com.itv.aws.lambda

import com.itv.aws.RoleName
import com.itv.aws.iam.CreateRoleRequest

object LambdaRoles {
  val SBT_AWS_LAMBDA_ROLE_NAME = RoleName("SBT_AWS_LAMBDA")
  val SBT_AWS_LAMBDA_POLICY_DOCUMENT =
    """{"Version":"2012-10-17","Statement":[{"Sid":"","Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":["sts:AssumeRole","ec2:CreateNetworkInterface", "ec2:DescribeNetworkInterfaces","ec2:DeleteNetworkInterface","cloudwatch:*"]}]}"""

  val createRoleRequest = CreateRoleRequest(
    name = SBT_AWS_LAMBDA_ROLE_NAME,
    policyDocument = SBT_AWS_LAMBDA_POLICY_DOCUMENT
  )
}
