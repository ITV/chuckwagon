package com.itv.chuckwagon.lambda

import com.amazonaws.services.lambda.runtime.Context

trait LambdaConfigForEnv {
  val env: String
}

case class LambdaConfig[T <: LambdaConfigForEnv](configs: T*) {
  def configFor(context: Context): T = {
    val arnSuffix = context.getInvokedFunctionArn.substring(context.getInvokedFunctionArn.lastIndexOf(":") + 1)

    configs
      .find(_.env == arnSuffix)
      .getOrElse(throw new IllegalArgumentException(s"Unknown environment '$arnSuffix'"))
  }
}
