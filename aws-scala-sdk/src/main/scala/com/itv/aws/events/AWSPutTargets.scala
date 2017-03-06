package com.itv.aws.events

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents
import com.itv.aws.AWSService
import com.amazonaws.services.cloudwatchevents.model.{Target, PutTargetsRequest => AWSPutTargetsRequest}
import com.itv.aws.iam.ARN

case class PutTargetsRequest(eventRule: EventRule, targetARN: ARN)
case class PutTargetsResponse()

class AWSPutTargets(events: AmazonCloudWatchEvents) extends AWSService[PutTargetsRequest, PutTargetsResponse] {
  override def apply(putTargetsRequest: PutTargetsRequest): PutTargetsResponse = {
    import putTargetsRequest._

    val awsAWSPutTargetsRequest = new AWSPutTargetsRequest()
      .withRule(eventRule.name.value)
      .withTargets(new Target().withId("1").withArn(targetARN.value))

    val _ = events.putTargets(awsAWSPutTargetsRequest)

    PutTargetsResponse()
  }
}
