package com.itv.chuckwagon.lambda

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import io.circe.generic.auto._
import scala.io.Source
import io.circe.parser._

class EventsDecodingTests extends FlatSpec with Matchers {

  val cronTriggerEventString =
    Source.fromResource("com/itv/chuckwagon/lambda/CronTriggerEvent.json").mkString

  "CronTriggeredEvent json string" should "decode to case class correctly" in {
    decode[Option[CronTriggeredEvent]](cronTriggerEventString).right.get shouldEqual Option(
      CronTriggeredEvent(
        "0",
        "some-unique-id",
        "Scheduled Event",
        "aws.events",
        "123456789012",
        "2017-05-23T15:53:34Z",
        "eu-west-1",
        List(
          "the-arn-that-triggered-it"
        )
      )
    )
  }

}
