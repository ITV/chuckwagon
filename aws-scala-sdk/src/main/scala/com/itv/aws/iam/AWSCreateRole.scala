package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.model.{CreateRoleRequest => AWSCreateRoleRequest}
import com.itv.aws.AWSService

case class CreateRoleRequest(roleDeclaration: RoleDeclaration)
case class CreateRoleResponse(role: Role)

class AWSCreateRole(iam: AmazonIdentityManagement) extends AWSService[CreateRoleRequest, CreateRoleResponse] {

  override def apply(
      createRoleRequest: CreateRoleRequest
  ): CreateRoleResponse = {

    import createRoleRequest._

    val awsCreateRoleRequest =
      new AWSCreateRoleRequest()
        .withRoleName(roleDeclaration.name.value)
        .withAssumeRolePolicyDocument(roleDeclaration.assumeRolePolicyDocument.value)

    val awsRole = iam.createRole(awsCreateRoleRequest).getRole
    val role = Role(
      roleDeclaration = roleDeclaration,
      arn = ARN(awsRole.getArn)
    )

    CreateRoleResponse(role)
  }
}
