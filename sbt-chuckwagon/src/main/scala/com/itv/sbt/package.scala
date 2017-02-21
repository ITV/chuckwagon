package com.itv.sbt

import com.itv.aws.lambda.AliasName
import sbt._

case class Environment(name: String) {
  val configuration = config(name) extend (Test)
  val aliasName = AliasName(name)
}
