package com.itv.aws

import com.amazonaws.services.cloudwatchevents.{AmazonCloudWatchEvents, AmazonCloudWatchEventsClientBuilder}
import com.itv.aws.iam.ARN

package object events {

  val events: AmazonCloudWatchEvents =
    AmazonCloudWatchEventsClientBuilder
      .standard()
      .withCredentials(provider)
      .build()
}

package events {

  case class RuleName(value: String) extends AnyVal

  case class ScheduleExpression(value: String) extends AnyVal

  case class EventRule(name: RuleName, scheduleExpression: ScheduleExpression, description: String)
  case class CreatedEventRule(eventRule: EventRule, arn: ARN)
}
