package com.itv.aws.s3

import java.io.File

import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest}
import com.itv.aws.AWSService


case class PutFileRequest(bucket: Bucket, keyPrefix: S3KeyPrefix, file:File)
case class PutFileResponse(key: S3Location)

object PutFile extends AWSService[PutFileRequest, PutFileResponse] {
  override def apply(putFileRequest: PutFileRequest): PutFileResponse = {
    import putFileRequest._

    val key: String = keyPrefix + file.getName
    val putObjectRequest = new PutObjectRequest(bucket.name, key, file)
    putObjectRequest.setCannedAcl(CannedAccessControlList.AuthenticatedRead)

    val putObjectResponse = awsS3.putObject(putObjectRequest)

    PutFileResponse(S3Location(S3Key(key), bucket))
  }
}
