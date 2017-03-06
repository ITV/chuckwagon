package com.itv.aws.s3

import java.io.{File, InputStream}

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{
  CannedAccessControlList,
  ObjectMetadata,
  PutObjectRequest => AWSPutObjectRequest
}
import com.itv.aws.AWSService

sealed trait PutObjectType
case class PutFile(keyPrefix: S3KeyPrefix, file: File)                                     extends PutObjectType
case class PutInputStream(s3Key: S3Key, inputStream: InputStream, inputStreamLength: Long) extends PutObjectType

case class PutObjectRequest(bucket: Bucket, putObject: PutObjectType)
case class PutObjectResponse(key: S3Location)

class AWSPutObject(awsS3: AmazonS3) extends AWSService[PutObjectRequest, PutObjectResponse] {
  override def apply(putFileRequest: PutObjectRequest): PutObjectResponse = {
    import putFileRequest._

    val (s3Key, awsPutObjectRequest) = putObject match {
      case PutFile(keyPrefix, file) => {
        val key: String = keyPrefix.value + file.getName
        (S3Key(key), new AWSPutObjectRequest(bucket.name.value, key, file))
      }
      case PutInputStream(s3Key, inputStream, inputStreamLength) => {

        val objectMetadata = new ObjectMetadata()
        objectMetadata.setContentLength(inputStreamLength)

        (s3Key, new AWSPutObjectRequest(bucket.name.value, s3Key.value, inputStream, objectMetadata))
      }
    }
    awsPutObjectRequest.setCannedAcl(CannedAccessControlList.AuthenticatedRead)

    val _ = awsS3.putObject(awsPutObjectRequest)

    PutObjectResponse(S3Location(bucket, s3Key))
  }
}
