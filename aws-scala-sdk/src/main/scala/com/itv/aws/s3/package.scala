package com.itv.aws

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

package object s3 {

  case class S3KeyPrefix(value: String) extends AnyVal
  case class S3Key(value: String) extends AnyVal

  case class S3Location(key: S3Key, bucket:Bucket)

  case class Bucket(name: String)

  val awsS3: AmazonS3 =
    com.itv.aws.configuredClient(AmazonS3ClientBuilder.standard())
}
