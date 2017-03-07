package com.itv.aws.sts

import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.model.{AssumeRoleRequest => AWSAssumeRoleRequest}
import com.itv.aws.AWSService
import com.itv.aws.iam.ARN

case class AssumeRoleRequest(roleARN: ARN, sessionName: AssumeRoleSessionName)
case class AssumeRoleResponse(credentials: Credentials)

class AWSAssumeRole(sts: AWSSecurityTokenService) extends AWSService[AssumeRoleRequest, AssumeRoleResponse] {
  override def apply(assumeRoleRequest: AssumeRoleRequest): AssumeRoleResponse = {
    import assumeRoleRequest._

    val awsAssumeRoleRequest = new AWSAssumeRoleRequest()
      .withRoleArn(roleARN.value)
      .withRoleSessionName(sessionName.value)

    val c = sts.assumeRole(awsAssumeRoleRequest).getCredentials
    AssumeRoleResponse(
      Credentials(
        accessKeyId = AccessKeyId(c.getAccessKeyId),
        secretAccessKey = SecretAccessKey(c.getSecretAccessKey),
        sessionToken = SessionToken(c.getSessionToken)
      ))
  }
}
