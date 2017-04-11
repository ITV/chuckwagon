package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.{DeleteAliasRequest => AWSDeleteAliasRequest}

class AWSDeleteAlias(awsLambda: AWSLambda) {
  def apply(
      alias: Alias
  ): AliasName = {

    val awsDeleteAliasRequest = new AWSDeleteAliasRequest()
      .withFunctionName(alias.lambdaName.value)
      .withName(alias.name.value)

    val _ = awsLambda.deleteAlias(awsDeleteAliasRequest)

    alias.name
  }
}
