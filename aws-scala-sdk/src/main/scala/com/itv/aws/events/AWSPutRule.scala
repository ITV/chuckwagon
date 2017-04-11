package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.amazonaws.services.cloudwatchevents.model.{PutRuleRequest => AWSPutRuleRequest}
import com.itv.aws.iam.ARN

class AWSPutRule(events: AmazonCloudWatchEvents) {
  def apply(eventRule: EventRule): CreatedEventRule = {

    val awsPutRuleRequest = new AWSPutRuleRequest()
      .withName(eventRule.name.value)
      .withScheduleExpression(eventRule.scheduleExpression.value)
      .withDescription(eventRule.description)

    val ruleResponse = events.putRule(awsPutRuleRequest)
    CreatedEventRule(eventRule, ARN(ruleResponse.getRuleArn))
  }
}
