package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.LambdaHandler
import com.itv.aws.lambda.LambdaRuntimeConfiguration
import com.itv.aws.lambda.MemorySize
import com.itv.aws.s3.BucketName
import com.itv.aws.s3.S3KeyPrefix
import com.itv.chuckwagon.deploy.VpcConfigLookup
import sbt.File
import sbt.TaskKey

import scala.concurrent.duration._

case class PublishLambdaConfiguration(roleARN: Option[ARN],
                                      vpcConfigLookup: Option[VpcConfigLookup],
                                      lambdaRuntimeConfiguration: LambdaRuntimeConfiguration,
                                      jarStagingBucketName: BucketName,
                                      jarStagingS3KeyPrefix: S3KeyPrefix,
                                      codeFile: TaskKey[File]) {}

object PublishLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_handler
  abstract class UNDEFINED_timeout
  abstract class UNDEFINED_memorySize
  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_codeFile

  implicit def getPublishLambdaConfiguration(
      builder: PublishLambdaConfigurationBuilder[DEFINED, DEFINED, DEFINED, DEFINED, DEFINED]
  ): PublishLambdaConfiguration =
    PublishLambdaConfiguration(
      builder.roleARN,
      builder.vpcConfigDeclaration,
      LambdaRuntimeConfiguration(
        builder.handler.get,
        builder.timeout.get,
        builder.memorySize.get,
        builder.deadLetterARN
      ),
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.codeFile.get
    )

  def apply() =
    new PublishLambdaConfigurationBuilder[
      UNDEFINED_handler,
      UNDEFINED_timeout,
      UNDEFINED_memorySize,
      UNDEFINED_stagingBucketName,
      UNDEFINED_codeFile
    ](None, None, None, None, None, None, None, None, None)
}

class PublishLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                        B_TIMEOUT,
                                        B_MEMORY_SIZE,
                                        B_STAGING_BUCKET_NAME,
                                        B_CODE_FILE](
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigLookup],
    val handler: Option[LambdaHandler],
    val timeout: Option[FiniteDuration],
    val memorySize: Option[MemorySize],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val deadLetterARN: Option[ARN],
    val codeFile: Option[TaskKey[File]]
) {
  def withRoleARN(arn: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      Option(ARN(arn)),
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withVpc(vpcConfigDeclaration: VpcConfigLookup) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      Option(vpcConfigDeclaration),
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withHandler(handler: String) =
    new PublishLambdaConfigurationBuilder[
      DEFINED,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      Option(LambdaHandler(handler)),
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withTimeout(timeout: String) = {
    val parsedDuration: FiniteDuration = Duration(timeout).asInstanceOf[FiniteDuration]
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      DEFINED,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      handler,
      Option(parsedDuration),
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )
  }

  def withMemorySizeInMB(memorySize: Int) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      DEFINED,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      Option(MemorySize(memorySize)),
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withStagingBucketName(name: String) =
    new PublishLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, B_MEMORY_SIZE, DEFINED, B_CODE_FILE](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withStagingBucketKeyPrefix(name: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      Option(S3KeyPrefix(name)),
      deadLetterARN,
      codeFile
    )

  def withDeadLetterARN(arn: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      Option(ARN(arn)),
      codeFile
    )

  def withCodeFile(codeFileTask: TaskKey[File]) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_HANDLER,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      DEFINED
    ](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      Option(codeFileTask)
    )
}
