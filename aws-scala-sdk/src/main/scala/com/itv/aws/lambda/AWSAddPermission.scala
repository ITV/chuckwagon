package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{AddPermissionRequest => AWSAddPermissionRequest}
import com.itv.aws.AWSService

case class AddPermissionRequest(alias: Alias, lambdaPermission: LambdaPermission)

case class AddPermissionResponse()

class AWSAddPermission(awsLambda: AWSLambda)
  extends AWSService[AddPermissionRequest, AddPermissionResponse] {

  override def apply(addPermissionRequest: AddPermissionRequest): AddPermissionResponse = {
    import addPermissionRequest._
    import addPermissionRequest.lambdaPermission._

    val awsAddPermissionRequest = new AWSAddPermissionRequest()
      .withFunctionName(alias.lambdaName.value)
      .withQualifier(alias.name.value)
      .withStatementId(statementId.value)
      .withAction(action.value)
      .withPrincipal(principalService.value)
      .withSourceArn(sourceARN.value)

    val _ = awsLambda.addPermission(awsAddPermissionRequest)

    AddPermissionResponse()
  }
}