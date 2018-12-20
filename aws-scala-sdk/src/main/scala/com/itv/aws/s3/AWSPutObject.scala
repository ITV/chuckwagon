package com.itv.aws.s3

import java.io.File
import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.{PutObjectRequest => AWSPutObjectRequest}

sealed trait PutObjectType
case class PutFile(keyPrefix: S3KeyPrefix, file: File)                                     extends PutObjectType
case class PutInputStream(s3Key: S3Key, inputStream: InputStream, inputStreamLength: Long) extends PutObjectType

class AWSPutObject(awsS3: AmazonS3) {
  def apply(bucket: Bucket, putObject: PutObjectType): S3Location = {

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
    S3Location(bucket, s3Key)
  }
}
