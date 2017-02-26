package com.itv

import com.amazonaws.auth.{
  AWSCredentialsProvider,
  DefaultAWSCredentialsProviderChain
}
import com.amazonaws.client.builder.AwsSyncClientBuilder
import com.amazonaws.regions.Regions

package object aws {

  val provider: AWSCredentialsProvider =
    new DefaultAWSCredentialsProviderChain()

  def configuredClientForRegion[Builder <: AwsSyncClientBuilder[
    Builder,
    Client
    ], Client](
                builder: AwsSyncClientBuilder[Builder, Client]
              ): Regions => Client = { (region: Regions) =>
  {
    builder.withCredentials(provider).withRegion(region).build()
  }
  }

  def configuredClient[Builder <: AwsSyncClientBuilder[Builder, Client],
  Client](
           builder: AwsSyncClientBuilder[Builder, Client]
         ): Client = {
    builder.withCredentials(provider).build()
  }
}

package aws {

  trait AWSService[Req, Res] extends (Req => Res)

  case class ARN(value: String) extends AnyVal

  case class RoleName(value: String) extends AnyVal

  case class Role(name: RoleName, arn: ARN)

}