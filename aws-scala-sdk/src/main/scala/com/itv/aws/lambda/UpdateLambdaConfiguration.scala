package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest
import com.itv.aws.AWSService


case class UpdateLambdaConfigurationRequest(lambda: Lambda)
case class UpdateLambdaConfigurationResponse()


class UpdateLambdaConfiguration(awsLambda: AWSLambda) extends AWSService[UpdateLambdaConfigurationRequest, UpdateLambdaConfigurationResponse] {
  override def apply(updateLambdaConfigurationRequest: UpdateLambdaConfigurationRequest): UpdateLambdaConfigurationResponse = {
    import updateLambdaConfigurationRequest.lambda._

    val awsUpdateFunctionConfigurationRequest = new UpdateFunctionConfigurationRequest().
      withFunctionName(name.value).
      withRole(configuration.roleARN.value).
      withHandler(configuration.handler.value).
      withTimeout(configuration.timeout.toSeconds.toInt).
      withMemorySize(configuration.memorySize.value)

    val _ = awsLambda.updateFunctionConfiguration(awsUpdateFunctionConfigurationRequest)

    UpdateLambdaConfigurationResponse()
  }
}
