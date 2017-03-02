package com.itv.aws.iam

import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class ListRolesRequest()

case class ListRolesResponse(roles: List[Role])

object AWSListRoles extends AWSService[ListRolesRequest, ListRolesResponse] {

  override def apply(listRolesRequest: ListRolesRequest): ListRolesResponse = {
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

    ListRolesResponse(roles)
  }
}
