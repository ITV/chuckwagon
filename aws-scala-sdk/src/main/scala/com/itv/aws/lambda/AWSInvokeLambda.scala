package com.itv.aws.lambda

import java.nio.ByteBuffer

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.InvokeRequest
import com.itv.aws.AWSService

sealed trait InvokeQualifier
case class VersionQualifier(version: LambdaVersion) extends InvokeQualifier {
  override def toString: String = version.value.toString
}
case class EnvironmentQualifier(aliasName: AliasName) extends InvokeQualifier {
  override def toString: String = aliasName.value
}

sealed trait LambdaResponse
case object HandledLambdaError extends LambdaResponse {
  override def toString(): String =
    "Handled Error"
}
case class UnhandledLambdaError(value: ByteBuffer) extends LambdaResponse {
  override def toString(): String =
    "Unhandled Error: " + new String(value.array(), "utf-8")
}
case class LambdaResponsePayload(value: ByteBuffer) extends LambdaResponse {
  override def toString(): String =
    new String(value.array(), "utf-8")
}

case class InvokeLambdaRequest(lambdaName: LambdaName, qualifier: Option[InvokeQualifier])
case class InvokeLambdaResponse(response: LambdaResponse)

class AWSInvokeLambda(awsLambda: AWSLambda) extends AWSService[InvokeLambdaRequest, InvokeLambdaResponse] {
  override def apply(invokeLambdaRequest: InvokeLambdaRequest): InvokeLambdaResponse = {
    import invokeLambdaRequest._

    val awsInvokeRequest = new InvokeRequest()
      .withFunctionName(lambdaName.value)

    qualifier.foreach(invokeQualifier => awsInvokeRequest.setQualifier(invokeQualifier.toString))

    val awsInvokeResponse = awsLambda.invoke(awsInvokeRequest)

    val response: LambdaResponse = awsInvokeResponse.getFunctionError match {
      case "Handled"   => HandledLambdaError
      case "Unhandled" => UnhandledLambdaError(awsInvokeResponse.getPayload)
      case _           => LambdaResponsePayload(awsInvokeResponse.getPayload)
    }

    InvokeLambdaResponse(response)
  }
}
