package com.itv.aws

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder

package object s3 {

  def s3: AwsClientBuilder[AmazonS3] = configuredClientForRegion(AmazonS3ClientBuilder.standard())

}

package s3 {
  case class S3KeyPrefix(value: String) extends AnyVal

  object S3KeyPrefix {
    def toString$extension1(unBoxed: String): String = unBoxed
  }

  case class S3Key(value: String) extends AnyVal

  object S3Key {
    def toString$extension1(unBoxed: String): String = unBoxed
  }

  case class S3Address(bucketName: BucketName, keyPrefix: S3KeyPrefix)

  case class S3Location(bucket: Bucket, key: S3Key)

  case class BucketName(value: String) extends AnyVal

  object BucketName {
    def toString$extension1(unBoxed: String): String = unBoxed
  }

  case class Bucket(name: BucketName)

}
