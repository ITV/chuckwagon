package com.itv.aws

import com.amazonaws.services.identitymanagement.{
  AmazonIdentityManagement,
  AmazonIdentityManagementClientBuilder
}

package object iam {

  val iam: AmazonIdentityManagement =
    AmazonIdentityManagementClientBuilder
      .standard()
      .withCredentials(provider)
      .build()

}
