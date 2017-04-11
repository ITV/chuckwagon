package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.model.{CreateRoleRequest => AWSCreateRoleRequest}

class AWSCreateRole(iam: AmazonIdentityManagement) {

  def apply(
      roleDeclaration: RoleDeclaration
  ): Role = {

    val awsCreateRoleRequest =
      new AWSCreateRoleRequest()
        .withRoleName(roleDeclaration.name.value)
        .withAssumeRolePolicyDocument(roleDeclaration.assumeRolePolicyDocument.value)

    val awsRole = iam.createRole(awsCreateRoleRequest).getRole
    Role(
      roleDeclaration = roleDeclaration,
      arn = ARN(awsRole.getArn)
    )
  }
}
