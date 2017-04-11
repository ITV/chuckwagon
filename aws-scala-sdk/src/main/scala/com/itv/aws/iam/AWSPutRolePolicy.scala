package com.itv.aws.iam

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.model.{PutRolePolicyRequest => AWSPutRolePolicyRequest}

class AWSPutRolePolicy(iam: AmazonIdentityManagement) {

  def apply(rolePolicy: RolePolicy): Role = {

    import rolePolicy._

    val awsPutRolePolicyRequest =
      new AWSPutRolePolicyRequest()
        .withRoleName(role.roleDeclaration.name.value)
        .withPolicyName(name.value)
        .withPolicyDocument(policyDocument.value)

    iam.putRolePolicy(awsPutRolePolicyRequest)

    role
  }
}
