package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.itv.aws.AWSService
import com.amazonaws.services.cloudwatchevents.model.{DeleteRuleRequest => AWSDeleteRuleRequest}

case class DeleteRuleRequest(ruleName: RuleName)
case class DeleteRuleResponse()

class AWSDeleteRule(events: AmazonCloudWatchEvents) extends AWSService[DeleteRuleRequest, DeleteRuleResponse] {
  override def apply(deleteRuleRequest: DeleteRuleRequest): DeleteRuleResponse = {
    import deleteRuleRequest._

    val awsPutRuleRequest = new AWSDeleteRuleRequest()
      .withName(ruleName.value)

    val _ = events.deleteRule(awsPutRuleRequest)
    DeleteRuleResponse()
  }
}
