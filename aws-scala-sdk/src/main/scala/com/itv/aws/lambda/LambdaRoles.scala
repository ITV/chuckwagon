package com.itv.aws.lambda

import com.itv.aws.{Role, RoleName}
import com.itv.aws.iam._

object LambdaRoles {

  def roleNameFor(lambdaName: LambdaName) =
    RoleName(s"lambda-chuckwagon-${lambdaName.value}")

  def roleDeclarationFor(lambdaName: LambdaName) =
    RoleDeclaration(
      name = roleNameFor(lambdaName),
      assumeRolePolicyDocument = ASSUME_ROLE_POLICY_DOCUMENT
    )

  val ASSUME_ROLE_POLICY_DOCUMENT =
    AssumeRolePolicyDocument(
      """{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}""")

  def rolePolicyFor(role: Role) =
    RolePolicy(
      name = RolePolicyName("chuckwagon-policy"),
      role = role,
      policyDocument = POLICY_DOCUMENT
    )

  val POLICY_DOCUMENT =
    RolePolicyDocument(
      """{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":"ec2:CreateNetworkInterface","Resource":"*"},{"Effect":"Allow","Action":"ec2:DescribeNetworkInterfaces","Resource":"*"},{"Effect":"Allow","Action":"ec2:DeleteNetworkInterface","Resource":"*"},{"Effect":"Allow","Action":"logs:*","Resource":"*"}]}""")

  // lots of lessons to adopt here, http://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/iam-identity-based-access-control-cwl.html
}
