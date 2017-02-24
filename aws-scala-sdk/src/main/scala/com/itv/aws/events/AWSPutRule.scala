package com.itv.aws.events

import com.itv.aws.AWSService
import com.amazonaws.services.cloudwatchevents.model.{PutRuleRequest => AWSPutRuleRequest}


case class PutRuleRequest(name: RuleName, scheduleExpression: ScheduleExpression)
case class PutRuleResponse()

object AWSPutRule extends AWSService[PutRuleRequest, PutRuleResponse] {
  override def apply(putRuleRequest: PutRuleRequest): PutRuleResponse = {
    import putRuleRequest._

    val awsPutRuleRequest = new AWSPutRuleRequest().withName(name.value).withScheduleExpression(scheduleExpression.value)

    val _ = events.putRule(awsPutRuleRequest)
    PutRuleResponse()
  }
}
