package com.itv.aws.events

import com.itv.aws.{ARN, AWSService}
import com.amazonaws.services.cloudwatchevents.model.{Target, PutTargetsRequest => AWSPutTargetsRequest}

case class PutTargetsRequest(ruleName: RuleName, targetARN: ARN)
case class PutTargetsResponse()

object AWSPutTargets extends AWSService[PutTargetsRequest, PutTargetsResponse] {
  override def apply(putTargetsRequest: PutTargetsRequest): PutTargetsResponse = {
    import putTargetsRequest._

    val awsAWSPutTargetsRequest = new AWSPutTargetsRequest().withRule(ruleName.value).withTargets(new Target().withId("1").withArn(targetARN.value))

    val _ = events.putTargets(awsAWSPutTargetsRequest)

    PutTargetsResponse()
  }
}
