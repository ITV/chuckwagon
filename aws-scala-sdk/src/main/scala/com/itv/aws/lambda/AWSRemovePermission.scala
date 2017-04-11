package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{RemovePermissionRequest => AWSRemovePermissionRequest}

class AWSRemovePermission(awsLambda: AWSLambda) {

  def apply(alias: Alias, lambdaPermission: LambdaPermission): Unit = {

    val awsRemovePermissionRequest = new AWSRemovePermissionRequest()
      .withFunctionName(alias.lambdaName.value)
      .withStatementId(lambdaPermission.statementId.value)
      .withQualifier(alias.name.value)

    val _ = awsLambda.removePermission(awsRemovePermissionRequest)
  }
}
