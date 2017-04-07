package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.AliasName
import com.itv.aws.lambda.LambdaVersion
import com.itv.chuckwagon.sbt.Keys.Base.chuckEnvironments
import sbt.Def
import sbt.complete.DefaultParsers._
import sbt.complete.FixedSetExamples
import sbt.complete.Parser

object Parsers {

  val environmentArgParser: Def.Initialize[Parser[AliasName]] = Def.setting {
    val environments: List[String] = chuckEnvironments.value.map(_.name).toList

    val environmentStringParser: Parser[String] =
      environments.foldLeft[Parser[String]](failure("Invalid input."))((e1, e2) => e1 | e2)
    val environmentExamplesParser: Parser[AliasName] =
      token(environmentStringParser.examples(FixedSetExamples(environments))).map(AliasName)
    token(' ') ~> environmentExamplesParser
  }

  val versionArgParser: Def.Initialize[Parser[LambdaVersion]] = Def.setting {
    token(' ') ~> token(NatBasic).map(LambdaVersion)
  }

}
