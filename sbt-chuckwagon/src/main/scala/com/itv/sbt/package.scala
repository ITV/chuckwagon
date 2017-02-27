package com.itv.sbt

import com.itv.aws.lambda.AliasName

case class Environment(name: String) {
  val aliasName = AliasName(name)
}
