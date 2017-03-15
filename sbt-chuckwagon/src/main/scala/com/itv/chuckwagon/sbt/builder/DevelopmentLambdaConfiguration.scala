package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.{LambdaHandler, LambdaRuntimeConfiguration, MemorySize, VpcConfigDeclaration}
import com.itv.aws.s3.{BucketName, S3KeyPrefix}
import sbt.{File, TaskKey}

import scala.concurrent.duration._

case class DevelopmentLambdaConfiguration(roleARN: Option[ARN],
                                          vpcConfigDeclaration: Option[VpcConfigDeclaration],
                                          lambdaRuntimeConfiguration: LambdaRuntimeConfiguration,
                                          jarStagingBucketName: BucketName,
                                          jarStagingS3KeyPrefix: S3KeyPrefix,
                                          codeGenerator: TaskKey[File]) {}

object DevelopmentLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_handler
  abstract class UNDEFINED_timeout
  abstract class UNDEFINED_memorySize
  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_codeGenerator

  implicit def getDevelopmentLambdaConfiguration(
      builder: DevelopmentLambdaConfigurationBuilder[DEFINED, DEFINED, DEFINED, DEFINED, DEFINED])
    : DevelopmentLambdaConfiguration =
    DevelopmentLambdaConfiguration(
      builder.roleARN,
      builder.vpcConfigDeclaration,
      LambdaRuntimeConfiguration(builder.handler.get, builder.timeout.get, builder.memorySize.get),
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.codeGenerator.get
    )

  def apply() =
    new DevelopmentLambdaConfigurationBuilder[UNDEFINED_handler,
                                              UNDEFINED_timeout,
                                              UNDEFINED_memorySize,
                                              UNDEFINED_stagingBucketName,
                                              UNDEFINED_codeGenerator](None, None, None, None, None, None, None, None)
}

class DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                            B_TIMEOUT,
                                            B_MEMORY_SIZE,
                                            B_STAGING_BUCKET_NAME,
                                            B_CODE_GENERATOR](
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigDeclaration],
    val handler: Option[LambdaHandler],
    val timeout: Option[FiniteDuration],
    val memorySize: Option[MemorySize],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val codeGenerator: Option[TaskKey[File]]
) {
  def withRoleARN(arn: String) =
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              B_TIMEOUT,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](Option(ARN(arn)),
                                                                vpcConfigDeclaration,
                                                                handler,
                                                                timeout,
                                                                memorySize,
                                                                stagingBucketName,
                                                                stagingBucketKeyPrefix,
                                                                codeGenerator)

  def withVpc(vpcConfigDeclaration: VpcConfigDeclaration) =
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              B_TIMEOUT,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](roleARN,
                                                                Option(vpcConfigDeclaration),
                                                                handler,
                                                                timeout,
                                                                memorySize,
                                                                stagingBucketName,
                                                                stagingBucketKeyPrefix,
                                                                codeGenerator)

  def withHandler(handler: String) =
    new DevelopmentLambdaConfigurationBuilder[DEFINED,
                                              B_TIMEOUT,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](roleARN,
                                                                vpcConfigDeclaration,
                                                                Option(LambdaHandler(handler)),
                                                                timeout,
                                                                memorySize,
                                                                stagingBucketName,
                                                                stagingBucketKeyPrefix,
                                                                codeGenerator)

  def withTimeout(timeout: String) = {
    val parsedDuration: FiniteDuration = Duration(timeout).asInstanceOf[FiniteDuration]
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              DEFINED,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](roleARN,
                                                                vpcConfigDeclaration,
                                                                handler,
                                                                Option(parsedDuration),
                                                                memorySize,
                                                                stagingBucketName,
                                                                stagingBucketKeyPrefix,
                                                                codeGenerator)
  }

  def withMemorySizeInMB(memorySize: Int) = {
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              B_TIMEOUT,
                                              DEFINED,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](roleARN,
                                                                vpcConfigDeclaration,
                                                                handler,
                                                                timeout,
                                                                Option(MemorySize(memorySize)),
                                                                stagingBucketName,
                                                                stagingBucketKeyPrefix,
                                                                codeGenerator)
  }

  def withStagingBucketName(name: String) = {
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER, B_TIMEOUT, B_MEMORY_SIZE, DEFINED, B_CODE_GENERATOR](
      roleARN,
      vpcConfigDeclaration,
      handler,
      timeout,
      memorySize,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      codeGenerator)
  }

  def withStagingBucketKeyPrefix(name: String) = {
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              B_TIMEOUT,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              B_CODE_GENERATOR](roleARN,
                                                                vpcConfigDeclaration,
                                                                handler,
                                                                timeout,
                                                                memorySize,
                                                                stagingBucketName,
                                                                Option(S3KeyPrefix(name)),
                                                                codeGenerator)
  }

  def withCodeGenerator(codeGeneratorTask: TaskKey[File]) = {
    new DevelopmentLambdaConfigurationBuilder[B_LAMBDA_HANDLER,
                                              B_TIMEOUT,
                                              B_MEMORY_SIZE,
                                              B_STAGING_BUCKET_NAME,
                                              DEFINED](roleARN,
                                                       vpcConfigDeclaration,
                                                       handler,
                                                       timeout,
                                                       memorySize,
                                                       stagingBucketName,
                                                       stagingBucketKeyPrefix,
                                                       Option(codeGeneratorTask))
  }
}
