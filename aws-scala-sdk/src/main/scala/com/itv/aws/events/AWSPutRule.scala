package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.itv.aws.AWSService
import com.amazonaws.services.cloudwatchevents.model.{PutRuleRequest => AWSPutRuleRequest}
import com.itv.aws.iam.ARN

case class PutRuleRequest(eventRule: EventRule)
case class PutRuleResponse(createdEventRule: CreatedEventRule)

class AWSPutRule(events: AmazonCloudWatchEvents) extends AWSService[PutRuleRequest, PutRuleResponse] {
  override def apply(putRuleRequest: PutRuleRequest): PutRuleResponse = {
    import putRuleRequest._

    val awsPutRuleRequest = new AWSPutRuleRequest()
      .withName(eventRule.name.value)
      .withScheduleExpression(eventRule.scheduleExpression.value)
      .withDescription(eventRule.description)

    val ruleResponse = events.putRule(awsPutRuleRequest)
    PutRuleResponse(
      CreatedEventRule(eventRule, ARN(ruleResponse.getRuleArn))
    )
  }
}
