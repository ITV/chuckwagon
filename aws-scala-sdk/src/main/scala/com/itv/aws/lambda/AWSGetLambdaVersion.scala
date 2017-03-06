package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.GetFunctionRequest
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN
import scala.concurrent.duration._

case class GetLambdaVersionRequest(lambdaName: LambdaName, aliasName: AliasName)
case class GetLambdaVersionResponse(downloadablePublishedLambda: DownloadablePublishedLambda)

class AWSGetLambdaVersion(awsLambda: AWSLambda) extends AWSService[GetLambdaVersionRequest, GetLambdaVersionResponse] {

  override def apply(getFunctionVersionRequest: GetLambdaVersionRequest): GetLambdaVersionResponse = {
    import getFunctionVersionRequest._

    val awsGetFunction = new GetFunctionRequest()
      .withFunctionName(lambdaName.value)
      .withQualifier(aliasName.value)

    val f  = awsLambda.getFunction(awsGetFunction)
    val fc = f.getConfiguration

    GetLambdaVersionResponse(
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
              memorySize = MemorySize(fc.getMemorySize)
            )
          ),
          version = LambdaVersion(fc.getVersion.toInt),
          arn = ARN(fc.getFunctionArn)
        ),
        downloadableLocation = DownloadableLambdaLocation(f.getCode.getLocation)
      ))
  }

}
