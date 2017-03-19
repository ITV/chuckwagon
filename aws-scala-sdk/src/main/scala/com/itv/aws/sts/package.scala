package com.itv.aws

import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder

package object sts {

  def sts: AwsClientBuilder[AWSSecurityTokenService] =
    configuredClientForRegion(AWSSecurityTokenServiceClientBuilder.standard())

}

package sts {

  case class AssumeRoleSessionName(value: String) extends AnyVal

}
