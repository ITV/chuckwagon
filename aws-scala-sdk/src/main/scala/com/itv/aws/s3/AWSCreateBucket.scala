package com.itv.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception

class AWSCreateBucket(awsS3: AmazonS3) {
  def apply(
      name: BucketName
  ): Bucket =
    try {
      val awsBucketResponse = awsS3.createBucket(name.value)

      Bucket(BucketName(awsBucketResponse.getName))
    } catch {
      case e: AmazonS3Exception => {
        throw new Exception(s"Unable to create bucket with name '${name.value}'", e)
      }
    }
}
