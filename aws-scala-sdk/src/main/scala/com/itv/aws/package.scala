package com.itv

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.client.builder.AwsSyncClientBuilder
import com.amazonaws.regions.Regions

package object aws {

  def configuredClientForRegion[Builder <: AwsSyncClientBuilder[
    Builder,
    Client
  ], Client](
      builder: AwsSyncClientBuilder[Builder, Client]
  ): Regions => AWSCredentialsProvider => Client = { (region: Regions) => credentials: AWSCredentialsProvider =>
    {
      builder.withRegion(region).withCredentials(credentials).build
    }
  }

  type AwsClientBuilder[T] = Regions => AWSCredentialsProvider => T
}

package aws {

  import com.amazonaws.auth.AWSCredentials
  import com.amazonaws.auth.AWSCredentialsProvider
  import com.amazonaws.auth.BasicSessionCredentials

  case class AccessKeyId(value: String)     extends AnyVal
  case class SecretAccessKey(value: String) extends AnyVal
  case class SessionToken(value: String)    extends AnyVal

  case class StaticCredentialsProvider(accessKeyId: AccessKeyId,
                                       secretAccessKey: SecretAccessKey,
                                       sessionToken: SessionToken)
      extends AWSCredentialsProvider {

    private val awsCredentials: AWSCredentials =
      new BasicSessionCredentials(
        accessKeyId.value,
        secretAccessKey.value,
        sessionToken.value
      )

    override def getCredentials: AWSCredentials = awsCredentials

    override def refresh(): Unit = {}
  }

}
