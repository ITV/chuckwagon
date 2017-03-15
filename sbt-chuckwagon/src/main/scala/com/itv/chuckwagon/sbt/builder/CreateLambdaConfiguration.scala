package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.{LambdaHandler, LambdaRuntimeConfiguration, MemorySize, VpcConfigDeclaration}
import com.itv.aws.s3.{BucketName, S3KeyPrefix}
import sbt.{File, TaskKey}

import scala.concurrent.duration._

case class CreateLambdaConfiguration(roleARN: Option[ARN],
                                     vpcConfigDeclaration: Option[VpcConfigDeclaration],
                                     lambdaRuntimeConfiguration: LambdaRuntimeConfiguration,
                                     jarStagingBucketName: BucketName,
                                     jarStagingS3KeyPrefix: S3KeyPrefix,
                                     codeFile: TaskKey[File]) {}

object CreateLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_handler
  abstract class UNDEFINED_timeout
  abstract class UNDEFINED_memorySize
  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_codeFile

  implicit def getCreateLambdaConfiguration(
      builder: CreateLambdaConfigurationBuilder[DEFINED, DEFINED, DEFINED, DEFINED, DEFINED])
    : CreateLambdaConfiguration =
    CreateLambdaConfiguration(
      builder.roleARN,
      builder.vpcConfigDeclaration,
      LambdaRuntimeConfiguration(builder.handler.get, builder.timeout.get, builder.memorySize.get),
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.codeFile.get
    )

  def apply() =
    new CreateLambdaConfigurationBuilder[UNDEFINED_handler,
                                         UNDEFINED_timeout,
                                         UNDEFINED_memorySize,
                                         UNDEFINED_stagingBucketName,
                                         UNDEFINED_codeFile](None, None, None, None, None, None, None, None)
}

class CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, B_MEMORY_SIZE, B_STAGING_BUCKET_NAME, B_CODE_FILE](
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigDeclaration],
    val handler: Option[LambdaHandler],
    val timeout: Option[FiniteDuration],
    val memorySize: Option[MemorySize],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val codeFile: Option[TaskKey[File]]
) {
  def withRoleARN(arn: String) =
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                         B_TIMEOUT,
                                         B_MEMORY_SIZE,
                                         B_STAGING_BUCKET_NAME,
                                         B_CODE_FILE](Option(ARN(arn)),
                                                      vpcConfigDeclaration,
                                                      handler,
                                                      timeout,
                                                      memorySize,
                                                      stagingBucketName,
                                                      stagingBucketKeyPrefix,
                                                      codeFile)

  def withVpc(vpcConfigDeclaration: VpcConfigDeclaration) =
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                         B_TIMEOUT,
                                         B_MEMORY_SIZE,
                                         B_STAGING_BUCKET_NAME,
                                         B_CODE_FILE](roleARN,
                                                      Option(vpcConfigDeclaration),
                                                      handler,
                                                      timeout,
                                                      memorySize,
                                                      stagingBucketName,
                                                      stagingBucketKeyPrefix,
                                                      codeFile)

  def withHandler(handler: String) =
    new CreateLambdaConfigurationBuilder[DEFINED, B_TIMEOUT, B_MEMORY_SIZE, B_STAGING_BUCKET_NAME, B_CODE_FILE](
      roleARN,
      vpcConfigDeclaration,
      Option(LambdaHandler(handler)),
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      codeFile)

  def withTimeout(timeout: String) = {
    val parsedDuration: FiniteDuration = Duration(timeout).asInstanceOf[FiniteDuration]
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER, DEFINED, B_MEMORY_SIZE, B_STAGING_BUCKET_NAME, B_CODE_FILE](
      roleARN,
      vpcConfigDeclaration,
      handler,
      Option(parsedDuration),
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      codeFile)
  }

  def withMemorySizeInMB(memorySize: Int) = {
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, DEFINED, B_STAGING_BUCKET_NAME, B_CODE_FILE](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      Option(MemorySize(memorySize)),
      stagingBucketName,
      stagingBucketKeyPrefix,
      codeFile)
  }

  def withStagingBucketName(name: String) = {
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, B_MEMORY_SIZE, DEFINED, B_CODE_FILE](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      codeFile)
  }

  def withStagingBucketKeyPrefix(name: String) = {
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                         B_TIMEOUT,
                                         B_MEMORY_SIZE,
                                         B_STAGING_BUCKET_NAME,
                                         B_CODE_FILE](roleARN,
                                                      vpcConfigDeclaration,
                                                      handler,
                                                      timeout,
                                                      memorySize,
                                                      stagingBucketName,
                                                      Option(S3KeyPrefix(name)),
                                                      codeFile)
  }

  def withCodeFile(codeFileTask: TaskKey[File]) = {
    new CreateLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, B_MEMORY_SIZE, B_STAGING_BUCKET_NAME, DEFINED](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      Option(codeFileTask))
  }
}
