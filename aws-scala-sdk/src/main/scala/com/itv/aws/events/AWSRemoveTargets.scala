package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.amazonaws.services.cloudwatchevents.model.{RemoveTargetsRequest => AWSRemoveTargetsRequest}

class AWSRemoveTargets(events: AmazonCloudWatchEvents) {
  def apply(ruleName: RuleName): Unit = {

    val awsRemoteTargetsRequest = new AWSRemoveTargetsRequest()
      .withRule(ruleName.value)
      .withIds(RULE_TARGET_ID)

    val _ = events.removeTargets(awsRemoteTargetsRequest)
  }
}
