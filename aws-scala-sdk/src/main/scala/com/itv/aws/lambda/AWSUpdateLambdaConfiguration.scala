package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.{VpcConfig => AWSVpcConfig}

import scala.collection.JavaConverters._

class AWSUpdateLambdaConfiguration(awsLambda: AWSLambda) {
  def apply(lambda: Lambda): Unit = {
    import lambda._
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
  }
}
