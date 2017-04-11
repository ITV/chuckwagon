package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.amazonaws.services.cloudwatchevents.model.Target
import com.amazonaws.services.cloudwatchevents.model.{PutTargetsRequest => AWSPutTargetsRequest}
import com.itv.aws.iam.ARN

class AWSPutTargets(events: AmazonCloudWatchEvents) {
  def apply(eventRule: EventRule, targetARN: ARN): Unit = {

    val awsAWSPutTargetsRequest = new AWSPutTargetsRequest()
      .withRule(eventRule.name.value)
      .withTargets(new Target().withId(RULE_TARGET_ID).withArn(targetARN.value))

    val _ = events.putTargets(awsAWSPutTargetsRequest)
  }
}
