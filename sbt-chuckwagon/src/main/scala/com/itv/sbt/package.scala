package com.itv.sbt

import com.itv.aws.lambda.AliasName
import sbt._

import cats.data.NonEmptyList

case class Environment(name: String) {
  val configuration = config(name) extend (Test)
  val aliasName = AliasName(name)
}

object BlueGreenEnvironments {
  def apply(firstName: String,
            subsequentNames: String*): NonEmptyList[Environment] = {

    NonEmptyList.of(firstName, subsequentNames.toSeq: _*).flatMap { name =>
      NonEmptyList.of(Environment(s"blue-$name"), Environment(s"$name"))
    }
  }
}
