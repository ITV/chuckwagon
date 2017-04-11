package com.itv.aws.s3

import com.amazonaws.services.s3.AmazonS3

import scala.collection.JavaConverters._

class AWSListBuckets(awsS3: AmazonS3) {
  def apply(): List[Bucket] = {
    val buckets: List[Bucket] = awsS3.listBuckets.asScala.map { b =>
      Bucket(BucketName(b.getName))
    }.toList

    buckets
  }
}
