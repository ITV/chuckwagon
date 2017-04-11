package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{AddPermissionRequest => AWSAddPermissionRequest}

class AWSAddPermission(awsLambda: AWSLambda) {

  def apply(alias: Alias, lambdaPermission: LambdaPermission): Unit = {
    import lambdaPermission._

    val awsAddPermissionRequest = new AWSAddPermissionRequest()
      .withFunctionName(alias.lambdaName.value)
      .withQualifier(alias.name.value)
      .withStatementId(statementId.value)
      .withAction(action.value)
      .withPrincipal(principalService.value)
      .withSourceArn(sourceARN.value)

    val _ = awsLambda.addPermission(awsAddPermissionRequest)
  }
}
