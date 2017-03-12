package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.AliasName
import com.itv.chuckwagon.sbt.Keys.Base.chuckEnvironments
import sbt.Def
import sbt.complete.DefaultParsers._
import sbt.complete.{FixedSetExamples, Parser}

import scala.collection.immutable.Seq

object Parsers {

  val environmentArgParser: Def.Initialize[Parser[AliasName]] = Def.setting {
    val environments: Seq[String] = chuckEnvironments.value.map(_.name).toList
    val environmentParser: Parser[AliasName] =
      token(NotQuoted.examples(FixedSetExamples(environments))).map(AliasName)
    token(' ') ~> environmentParser
  }

}
