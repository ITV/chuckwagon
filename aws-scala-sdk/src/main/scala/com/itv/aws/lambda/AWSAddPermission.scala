package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{AddPermissionRequest => AWSAddPermissionRequest}
import com.itv.aws.{ARN, AWSService}

case class AddPermissionRequest(lambdaName: LambdaName, action: String, principal: String, sourceARN: ARN)
case class AddPermissionResponse()

class AWSAddPermission(awsLambda: AWSLambda)
  extends AWSService[AddPermissionRequest, AddPermissionResponse] {

  override def apply(addPermissionRequest: AddPermissionRequest): AddPermissionResponse = {
    import addPermissionRequest._

    val awsAddPermissionRequest = new AWSAddPermissionRequest().withFunctionName(lambdaName.value).withStatementId("statement id").withAction(action).withPrincipal(principal).withSourceArn(sourceARN.value)

    val _ = awsLambda.addPermission(awsAddPermissionRequest)

    AddPermissionResponse()
  }
}