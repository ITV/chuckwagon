package com.itv.aws.sts

import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.model.{AssumeRoleRequest => AWSAssumeRoleRequest}
import com.itv.aws._
import com.itv.aws.iam.ARN

class AWSAssumeRole(sts: AWSSecurityTokenService) {
  def apply(roleARN: ARN, sessionName: AssumeRoleSessionName): StaticCredentialsProvider = {

    val awsAssumeRoleRequest = new AWSAssumeRoleRequest()
      .withRoleArn(roleARN.value)
      .withRoleSessionName(sessionName.value)

    val c = sts.assumeRole(awsAssumeRoleRequest).getCredentials
    StaticCredentialsProvider(
      accessKeyId = AccessKeyId(c.getAccessKeyId),
      secretAccessKey = SecretAccessKey(c.getSecretAccessKey),
      sessionToken = SessionToken(c.getSessionToken)
    )
  }
}
