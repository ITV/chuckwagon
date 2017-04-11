package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.amazonaws.services.cloudwatchevents.model.{DeleteRuleRequest => AWSDeleteRuleRequest}

class AWSDeleteRule(events: AmazonCloudWatchEvents) {
  def apply(ruleName: RuleName): Unit = {

    val awsPutRuleRequest = new AWSDeleteRuleRequest()
      .withName(ruleName.value)

    val _ = events.deleteRule(awsPutRuleRequest)
  }
}
