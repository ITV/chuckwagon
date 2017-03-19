package com.itv.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.itv.aws.AWSService

case class CreateBucketRequest(name: BucketName)
case class CreateBucketResponse(bucket: Bucket)

class AWSCreateBucket(awsS3: AmazonS3) extends AWSService[CreateBucketRequest, CreateBucketResponse] {
  override def apply(
      createBucketRequest: CreateBucketRequest
  ): CreateBucketResponse =
    try {
      val awsBucketResponse = awsS3.createBucket(createBucketRequest.name.value)

      CreateBucketResponse(Bucket(BucketName(awsBucketResponse.getName)))
    } catch {
      case e: AmazonS3Exception => {
        throw new Exception(s"Unable to create bucket with name '${createBucketRequest.name.value}'", e)
      }
    }
}
