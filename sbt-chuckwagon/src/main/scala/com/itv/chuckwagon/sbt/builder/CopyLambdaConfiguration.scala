package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.LambdaName
import com.itv.aws.s3.BucketName
import com.itv.aws.s3.S3KeyPrefix
import com.itv.chuckwagon.deploy.VpcConfigLookup

case class CopyLambdaConfiguration(lambdaNames: List[LambdaName],
                                   roleARN: Option[ARN],
                                   vpcConfigLookup: Option[VpcConfigLookup],
                                   jarStagingBucketName: BucketName,
                                   jarStagingS3KeyPrefix: S3KeyPrefix,
                                   assumableDevAccountRoleARN: ARN)

object CopyLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_lambdaName
  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_assumableDevAccountRoleARN

  implicit def getCopyLambdaConfiguration(
      builder: CopyLambdaConfigurationBuilder[DEFINED, DEFINED, DEFINED]
  ): CopyLambdaConfiguration =
    CopyLambdaConfiguration(
      builder.lambdaNames,
      builder.roleARN,
      builder.vpcConfigDeclaration,
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.assumableDevAccountRoleARN.get
    )

  def apply() =
    new CopyLambdaConfigurationBuilder[
      UNDEFINED_lambdaName,
      UNDEFINED_stagingBucketName,
      UNDEFINED_assumableDevAccountRoleARN
    ](
      Nil,
      None,
      None,
      None,
      None,
      None
    )
}

class CopyLambdaConfigurationBuilder[B_NAME, B_STAGING_BUCKET_NAME, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
    val lambdaNames: List[LambdaName],
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigLookup],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val assumableDevAccountRoleARN: Option[ARN]
) {

  def withName(name: String) =
    new CopyLambdaConfigurationBuilder[
      DEFINED,
      B_STAGING_BUCKET_NAME,
      B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN
    ](
      List(LambdaName(name)),
      roleARN,
      vpcConfigDeclaration,
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN
    )

  def withNames(names: String*) =
    new CopyLambdaConfigurationBuilder[
      DEFINED,
      B_STAGING_BUCKET_NAME,
      B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN
    ](
      names.toList.map(LambdaName),
      roleARN,
      vpcConfigDeclaration,
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN
    )

  def withRoleARN(arn: String) =
    new CopyLambdaConfigurationBuilder[
      B_NAME,
      B_STAGING_BUCKET_NAME,
      B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN
    ](
      lambdaNames,
      Option(ARN(arn)),
      vpcConfigDeclaration,
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN
    )

  def withVpc(vpcConfigDeclaration: VpcConfigLookup) =
    new CopyLambdaConfigurationBuilder[
      B_NAME,
      B_STAGING_BUCKET_NAME,
      B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN
    ](
      lambdaNames,
      roleARN,
      Option(vpcConfigDeclaration),
      stagingBucketName,
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN
    )

  def withStagingBucketName(name: String) =
    new CopyLambdaConfigurationBuilder[B_NAME, DEFINED, B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN](
      lambdaNames,
      roleARN,
      vpcConfigDeclaration,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      assumableDevAccountRoleARN
    )

  def withStagingBucketKeyPrefix(name: String) =
    new CopyLambdaConfigurationBuilder[
      B_NAME,
      B_STAGING_BUCKET_NAME,
      B_STAGING_ASSUMABLE_DEV_ACCOUNT_ROLE_ARN
    ](
      lambdaNames,
      roleARN,
      vpcConfigDeclaration,
      stagingBucketName,
      Option(S3KeyPrefix(name)),
      assumableDevAccountRoleARN
    )

  def withAssumableDevAccountRoleARN(name: String) =
    new CopyLambdaConfigurationBuilder[B_NAME, B_STAGING_BUCKET_NAME, DEFINED](
      lambdaNames,
      roleARN,
      vpcConfigDeclaration,
      stagingBucketName,
      stagingBucketKeyPrefix,
      Option(ARN(name))
    )
}
