package com.itv.aws.lambda

import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.itv.aws.iam.ARN

object FunctionConfigurationHelpers {

  def deadLetterARN(fc: FunctionConfiguration): Option[ARN] = {
    val deadLetterConfig = fc.getDeadLetterConfig
    if (deadLetterConfig == null)
      None
    else {
      val rawARN = fc.getDeadLetterConfig.getTargetArn
      if (rawARN != null && !rawARN.isEmpty)
        Option(ARN(rawARN))
      else
        None
    }
  }
}
