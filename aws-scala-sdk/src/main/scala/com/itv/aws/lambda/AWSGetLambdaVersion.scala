package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.GetFunctionRequest
import com.itv.aws.iam.ARN

import scala.concurrent.duration._

class AWSGetLambdaVersion(awsLambda: AWSLambda) {
  def apply(lambdaName: LambdaName, aliasName: AliasName): DownloadablePublishedLambda = {

    val awsGetFunction = new GetFunctionRequest()
      .withFunctionName(lambdaName.value)
      .withQualifier(aliasName.value)

    val f  = awsLambda.getFunction(awsGetFunction)
    val fc = f.getConfiguration

    DownloadablePublishedLambda(
      publishedLambda = PublishedLambda(
        lambda = Lambda(
          deployment = LambdaDeploymentConfiguration(
            name = lambdaName,
            roleARN = ARN(fc.getRole),
            vpcConfig = None
          ),
          runtime = LambdaRuntimeConfiguration(
            handler = LambdaHandler(fc.getHandler),
            timeout = fc.getTimeout.toDouble.seconds,
            memorySize = MemorySize(fc.getMemorySize),
            deadLetterARN = FunctionConfigurationHelpers.deadLetterARN(fc)
          )
        ),
        version = LambdaVersion(fc.getVersion.toInt),
        arn = ARN(fc.getFunctionArn)
      ),
      downloadableLocation = DownloadableLambdaLocation(f.getCode.getLocation)
    )
  }

}
