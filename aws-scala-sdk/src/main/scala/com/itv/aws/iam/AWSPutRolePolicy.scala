package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.model.{PutRolePolicyRequest => AWSPutRolePolicyRequest}
import com.itv.aws.AWSService

case class PutRolePolicyRequest(rolePolicy: RolePolicy)
case class PutRolePolicyResponse(role: Role)

object AWSPutRolePolicy extends AWSService[PutRolePolicyRequest, PutRolePolicyResponse] {

  override def apply(putRolePolicyRequest: PutRolePolicyRequest): PutRolePolicyResponse = {

    import putRolePolicyRequest.rolePolicy._

    val awsPutRolePolicyRequest =
      new AWSPutRolePolicyRequest()
        .withRoleName(role.roleDeclaration.name.value)
        .withPolicyName(name.value)
        .withPolicyDocument(policyDocument.value)

    iam.putRolePolicy(awsPutRolePolicyRequest)

    PutRolePolicyResponse(role)
  }
}
