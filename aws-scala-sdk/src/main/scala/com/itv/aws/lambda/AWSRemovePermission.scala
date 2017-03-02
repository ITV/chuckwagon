package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{RemovePermissionRequest => AWSRemovePermissionRequest}
import com.itv.aws.AWSService

case class RemovePermissionRequest(alias: Alias, lambdaPermission: LambdaPermission)
case class RemovePermissionResponse()

class AWSRemovePermission(awsLambda: AWSLambda) extends AWSService[RemovePermissionRequest, RemovePermissionResponse] {

  override def apply(removePermissionRequest: RemovePermissionRequest): RemovePermissionResponse = {
    import removePermissionRequest._

    val awsRemovePermissionRequest = new AWSRemovePermissionRequest()
      .withFunctionName(alias.lambdaName.value)
      .withStatementId(lambdaPermission.statementId.value)
      .withQualifier(alias.name.value)

    val _ = awsLambda.removePermission(awsRemovePermissionRequest)

    RemovePermissionResponse()
  }
}
