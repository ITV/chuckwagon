package com.itv.aws.iam

import com.itv.aws.{ARN, AWSService, Role, RoleName}

import scala.collection.JavaConverters._

case class ListRolesRequest()

case class ListRolesResponse(roles: List[Role])

object ListRoles extends AWSService[ListRolesRequest, ListRolesResponse] {

  override def apply(listRolesRequest: ListRolesRequest): ListRolesResponse = {
    val roles = iam.listRoles().getRoles.asScala.map { r =>
      Role(
        name = RoleName(r.getRoleName),
        arn = ARN(r.getArn)
      )
    }.toList

    ListRolesResponse(roles)
  }
}
