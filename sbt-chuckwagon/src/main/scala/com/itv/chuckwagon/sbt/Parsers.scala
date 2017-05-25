package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.AliasName
import com.itv.aws.lambda.LambdaName
import com.itv.aws.lambda.LambdaVersion
import com.itv.chuckwagon.sbt.Keys.Base._
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

  val chuckNameParser: Def.Initialize[Parser[LambdaName]] = Def.setting {
    val names: List[String] = chuckNames.value.map(_.value)

    val nameStringParser: Parser[String] =
      names.foldLeft[Parser[String]](failure("Invalid input."))((e1, e2) => e1 | e2)
    val nameExamplesParser: Parser[LambdaName] =
      token(nameStringParser.examples(FixedSetExamples(names))).map(LambdaName)
    token(' ') ~> nameExamplesParser
  }

  val ScheduledEventRule = """{
                             |  "version": "0",
                             |  "id": "some-unique-id",
                             |  "detail-type": "Scheduled Event",
                             |  "source": "aws.events",
                             |  "account": "123456789012",
                             |  "time": "2017-05-23T15:53:34Z",
                             |  "region": "eu-west-1",
                             |  "resources": [
                             |    "the-arn-that-triggered-it"
                             |  ],
                             |  "detail": {}
                             |}""".stripMargin

  val payloadParser: Parser[String] = {
    token(' ') ~> ("ScheduledEventRule".examples(FixedSetExamples(List("ScheduledEventRule"))) || StringVerbatim
      .examples(FixedSetExamples(List("\"\"\"")))).map { options =>
      options.fold(
        {
          case "ScheduledEventRule" => ScheduledEventRule
        },
        payload => payload
      )
    }
  }

}
