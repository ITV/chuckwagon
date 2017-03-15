package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.{LambdaHandler, LambdaRuntimeConfiguration, MemorySize, VpcConfigDeclaration}
import com.itv.aws.s3.{BucketName, S3KeyPrefix}

import scala.concurrent.duration.{Duration, FiniteDuration}

case class ProductionLambdaConfiguration(roleARN: Option[ARN],
                                         vpcConfigDeclaration: Option[VpcConfigDeclaration],
                                         jarStagingBucketName: BucketName,
                                         jarStagingS3KeyPrefix: S3KeyPrefix,
                                         assumableDevAccountRoleARN: ARN)

object ProductionLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_assumableDevAccountRoleARN

  implicit def getProductionLambdaConfiguration(
      builder: ProductionLambdaConfigurationBuilder[DEFINED, DEFINED]): ProductionLambdaConfiguration =
    ProductionLambdaConfiguration(
      builder.roleARN,
      builder.vpcConfigDeclaration,
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.assumableDevAccountRoleARN.get
    )

  def apply() =
    new ProductionLambdaConfigurationBuilder[UNDEFINED_stagingBucketName, UNDEFINED_assumableDevAccountRoleARN](None,
                                                                                                                None,
                                                                                                                None,
                                                                                                                None,
                                                                                                                None)
}

class ProductionLambdaConfigurationBuilder[B_STAGING_BUCKET_NAME, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigDeclaration],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val assumableDevAccountRoleARN: Option[ARN]) {
  def withRoleARN(arn: String) =
    new ProductionLambdaConfigurationBuilder[B_STAGING_BUCKET_NAME, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
      Option(ARN(arn)),
      vpcConfigDeclaration,
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN)

  def withVpc(vpcConfigDeclaration: VpcConfigDeclaration) =
    new ProductionLambdaConfigurationBuilder[B_STAGING_BUCKET_NAME, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
      roleARN,
      Option(vpcConfigDeclaration),
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN)

  def withStagingBucketName(name: String) = {
    new ProductionLambdaConfigurationBuilder[DEFINED, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
      roleARN,
      vpcConfigDeclaration,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN)
  }

  def withStagingBucketKeyPrefix(name: String) = {
    new ProductionLambdaConfigurationBuilder[B_STAGING_BUCKET_NAME, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
      roleARN,
      vpcConfigDeclaration,
      stagingBucketName,
      Option(S3KeyPrefix(name)),
      assumableDevAccountRoleARN)
  }

  def withAssumableDevAccountRoleARN(name: String) = {
    new ProductionLambdaConfigurationBuilder[B_STAGING_BUCKET_NAME, DEFINED](roleARN,
                                                                             vpcConfigDeclaration,
                                                                             stagingBucketName,
                                                                             stagingBucketKeyPrefix,
                                                                             Option(ARN(name)))
  }
}
