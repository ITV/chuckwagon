package com.itv.sbt

import com.itv.aws.ARN
import com.itv.aws.lambda._
import sbt._

object AWSLambdaPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val awsLambdaDeploy = taskKey[ARN](
      "Deploy, test and put live the code in this project as an AWS Lambda"
    )
    val awsLambdaList = taskKey[String](
      "Deploy, test and put live the code in this project as an AWS Lambda"
    )
    val awsLambdaFunctionName = settingKey[String](
      "The name of the AWS Lambda Function in this project"
    )
  }
  import autoImport._

//  override lazy val projectSettings = Seq(
//    awsLambdaList := {
//      val functionName = LambdaName(awsLambdaFunctionName.value)
//
//      AwsLambda.listAliases(ListAliasesQuery(functionName)).toString
//    }
//  )
}
