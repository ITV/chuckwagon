package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.{VpcConfig => AWSVpcConfig}
import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class UpdateLambdaConfigurationRequest(lambda: Lambda)
case class UpdateLambdaConfigurationResponse()

class AWSUpdateLambdaConfiguration(awsLambda: AWSLambda)
    extends AWSService[
      UpdateLambdaConfigurationRequest,
      UpdateLambdaConfigurationResponse
    ] {
  override def apply(
      updateLambdaConfigurationRequest: UpdateLambdaConfigurationRequest
  ): UpdateLambdaConfigurationResponse = {
    import updateLambdaConfigurationRequest.lambda._
    import runtime._
    import deployment._

    val awsUpdateFunctionConfigurationRequest =
      new UpdateFunctionConfigurationRequest()
        .withFunctionName(name.value)
        .withRole(roleARN.value)
        .withHandler(handler.value)
        .withTimeout(timeout.toSeconds.toInt)
        .withMemorySize(memorySize.value)

    vpcConfig.foreach { vpc =>
      awsUpdateFunctionConfigurationRequest.withVpcConfig(
        new AWSVpcConfig()
          .withSecurityGroupIds(vpc.securityGroups.map(_.id).asJava)
          .withSubnetIds(vpc.subnets.map(_.id).asJava)
      )
    }

    val _ = awsLambda.updateFunctionConfiguration(
      awsUpdateFunctionConfigurationRequest
    )

    UpdateLambdaConfigurationResponse()
  }
}
