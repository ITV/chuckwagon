package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.itv.aws.AWSService
import com.amazonaws.services.cloudwatchevents.model.{RemoveTargetsRequest => AWSRemoveTargetsRequest}

case class RemoveTargetsRequest(ruleName: RuleName)
case class RemoveTargetsResponse()

class AWSRemoveTargets(events: AmazonCloudWatchEvents)
    extends AWSService[RemoveTargetsRequest, RemoveTargetsResponse] {
  override def apply(removeTargetsRequest: RemoveTargetsRequest): RemoveTargetsResponse = {
    import removeTargetsRequest._

    val awsRemoteTargetsRequest = new AWSRemoveTargetsRequest()
      .withRule(ruleName.value)
      .withIds(RULE_TARGET_ID)

    val _ = events.removeTargets(awsRemoteTargetsRequest)

    RemoveTargetsResponse()
  }
}
