package com.itv.aws.lambda

import java.nio.ByteBuffer

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.InvokeRequest

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

class AWSInvokeLambda(awsLambda: AWSLambda) {
  def apply(lambdaName: LambdaName, qualifier: Option[InvokeQualifier]): LambdaResponse = {

    val awsInvokeRequest = new InvokeRequest()
      .withFunctionName(lambdaName.value)

    qualifier.foreach(invokeQualifier => awsInvokeRequest.setQualifier(invokeQualifier.toString))

    val awsInvokeResponse = awsLambda.invoke(awsInvokeRequest)

    awsInvokeResponse.getFunctionError match {
      case "Handled"   => HandledLambdaError
      case "Unhandled" => UnhandledLambdaError(awsInvokeResponse.getPayload)
      case _           => LambdaResponsePayload(awsInvokeResponse.getPayload)
    }
  }
}
