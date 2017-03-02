package com.itv.aws

import com.amazonaws.services.identitymanagement.{AmazonIdentityManagement, AmazonIdentityManagementClientBuilder}

package object iam {

  val iam: AmazonIdentityManagement =
    AmazonIdentityManagementClientBuilder
      .standard()
      .withCredentials(provider)
      .build()

}

package iam {

  case class ARN(value: String) extends AnyVal

  case class RoleName(value: String) extends AnyVal

  case class Role(roleDeclaration: RoleDeclaration, arn: ARN)

  case class RolePolicy(name: RolePolicyName, role: Role, policyDocument: RolePolicyDocument)

  case class AssumeRolePolicyDocument(value: String) extends AnyVal
  case class RolePolicyDocument(value: String)       extends AnyVal
  case class RolePolicyName(value: String)           extends AnyVal
  case class RoleDeclaration(name: RoleName, assumeRolePolicyDocument: AssumeRolePolicyDocument)
}
