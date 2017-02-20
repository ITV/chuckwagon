package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.AWSService
import com.amazonaws.services.lambda.model.{
  DeleteAliasRequest => AWSDeleteAliasRequest
}

case class DeleteAliasRequest(alias: Alias)
case class DeleteAliasResponse(name: AliasName)

class AWSDeleteAlias(awsLambda: AWSLambda)
    extends AWSService[DeleteAliasRequest, DeleteAliasResponse] {
  override def apply(
    deleteAliasRequest: DeleteAliasRequest
  ): DeleteAliasResponse = {
    import deleteAliasRequest._

    val awsDeleteAliasRequest = new AWSDeleteAliasRequest()
      .withFunctionName(alias.lambdaName.value)
      .withName(alias.name.value)

    val _ = awsLambda.deleteAlias(awsDeleteAliasRequest)

    DeleteAliasResponse(alias.name)
  }
}
