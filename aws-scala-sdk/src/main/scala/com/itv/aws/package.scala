package com.itv

import com.amazonaws.auth.{AWSCredentialsProviderChain, AWSStaticCredentialsProvider}
import com.amazonaws.client.builder.AwsSyncClientBuilder
import com.amazonaws.regions.Regions
import com.itv.aws.sts.Credentials

package object aws {

  def configuredClientForRegion[Builder <: AwsSyncClientBuilder[
                                  Builder,
                                  Client
                                ],
                                Client](
      builder: AwsSyncClientBuilder[Builder, Client]
  ): Regions => Option[Credentials] => Client = { (region: Regions) => maybeSessionCredentials: Option[Credentials] =>
    {
      maybeSessionCredentials match {
        case None =>
          builder.withRegion(region).build
        case Some(sessionCredentials) => {
          val sessionCredentialsProvider =
            new AWSCredentialsProviderChain(new AWSStaticCredentialsProvider(sessionCredentials.awsCredentials))

          builder.withRegion(region).withCredentials(sessionCredentialsProvider).build()
        }
      }
    }
  }

  type AwsClientBuilder[T] = Regions => Option[Credentials] => T
}

package aws {

  trait AWSService[Req, Res] extends (Req => Res)

}
