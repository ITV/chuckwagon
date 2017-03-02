package com.itv.aws.s3

import com.itv.aws.AWSService

case class CreateBucketRequest(name: BucketName)
case class CreateBucketResponse(bucket: Bucket)

object AWSCreateBucket extends AWSService[CreateBucketRequest, CreateBucketResponse] {
  override def apply(
      createBucketRequest: CreateBucketRequest
  ): CreateBucketResponse = {
    val awsBucketResponse = awsS3.createBucket(createBucketRequest.name.value)

    CreateBucketResponse(Bucket(BucketName(awsBucketResponse.getName)))
  }
}
