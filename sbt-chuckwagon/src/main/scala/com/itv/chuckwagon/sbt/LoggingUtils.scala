package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.Lambda
import com.itv.aws.lambda.LambdaName
import fansi.Color._
import fansi.Str

object LoggingUtils {

  def logItemsMessage(lambdaName: LambdaName, prefix: String, items: String*): String = {

    val colouredItems =
      if (items.isEmpty) ""
      else
        items
          .map(Green(_).render)
          .mkString(Str("'").render, Str("', '").render, Str("'").render)

    (Cyan("Chuckwagon") ++ Str(s": ") ++ LightBlue(lambdaName.value) ++ Str(s": $prefix ")).render ++ colouredItems
  }

  def logMessage(message: String): String =
    (Cyan("Chuckwagon") ++ Str(s": ")).render ++ message

  def logMessage(lambda: Lambda, message: String): String =
    logMessage(lambda.deployment.name, message)

  def logMessage(lambdaName: LambdaName, message: String): String =
    (Cyan("Chuckwagon") ++ Str(s": ") ++ LightBlue(lambdaName.value) ++ Str(": ")).render ++ message
}
