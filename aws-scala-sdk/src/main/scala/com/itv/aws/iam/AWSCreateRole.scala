package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.model.{CreateRoleRequest => AWSCreateRoleRequest}
import com.itv.aws.{ARN, AWSService, Role, RoleName}


case class CreateRoleRequest(name: RoleName, policyDocument: String)
case class CreateRoleResponse(role: Role)

object AWSCreateRole extends AWSService[CreateRoleRequest, CreateRoleResponse] {

  override def apply(createRoleRequest: CreateRoleRequest): CreateRoleResponse = {
    val awsCreateRoleRequest =
      new AWSCreateRoleRequest().
        withRoleName(createRoleRequest.name.value).
        withAssumeRolePolicyDocument(createRoleRequest.policyDocument)

    val awsRole = iam.createRole(awsCreateRoleRequest).getRole

    CreateRoleResponse(Role(
      name = RoleName(awsRole.getRoleName),
      arn = ARN(awsRole.getArn)
    ))
  }
}
