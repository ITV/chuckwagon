package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement

import scala.collection.JavaConverters._

class AWSListRoles(iam: AmazonIdentityManagement) {

  def apply(): List[Role] = {
    val roles = iam
      .listRoles()
      .getRoles
      .asScala
      .map { r =>
        Role(
          roleDeclaration = RoleDeclaration(
            name = RoleName(r.getRoleName),
            assumeRolePolicyDocument = AssumeRolePolicyDocument(r.getAssumeRolePolicyDocument)
          ),
          arn = ARN(r.getArn)
        )
      }
      .toList

    roles
  }
}
