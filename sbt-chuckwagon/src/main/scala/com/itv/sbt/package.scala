package com.itv

import com.itv.aws.lambda.AliasName

package object sbt {

  sealed trait LambdaEnv {
    val aliasName: AliasName
    val isTestEnv: Boolean
  }

  case class BlueLambdaEnv(aliasName: AliasName) extends LambdaEnv {
    val isTestEnv = true
  }
  case class GreenLambdaEnv(aliasName: AliasName) extends LambdaEnv {
    val isTestEnv = false
  }
}
