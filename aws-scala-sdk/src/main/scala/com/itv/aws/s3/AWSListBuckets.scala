package com.itv.aws.s3

import com.itv.aws.AWSService

import scala.collection.JavaConverters._

case class ListBucketsRequest()
case class ListBucketsResponse(buckets: List[Bucket])

object AWSListBuckets extends AWSService[ListBucketsRequest, ListBucketsResponse] {
  override def apply(v1: ListBucketsRequest): ListBucketsResponse = {
    val buckets: List[Bucket] = awsS3.listBuckets.asScala.map { b =>
      Bucket(b.getName)
    }.toList

    ListBucketsResponse(buckets)
  }
}
