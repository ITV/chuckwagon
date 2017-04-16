package com.itv.chuckwagon.sbt.builder

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.LambdaHandler
import com.itv.aws.lambda.LambdaName
import com.itv.aws.lambda.MemorySize
import com.itv.aws.s3.BucketName
import com.itv.aws.s3.S3KeyPrefix
import com.itv.chuckwagon.deploy.VpcConfigLookup
import sbt.File
import sbt.TaskKey

import scala.concurrent.duration._

sealed trait NameAndHandlerType
case object SingleNameAndHandler     extends NameAndHandlerType
case object MultipleNamesAndHandlers extends NameAndHandlerType

case class LambdaNameToHandlerMapping(lambdaName: LambdaName, lambdaHandler: LambdaHandler)

case class PublishLambdaConfiguration(lambdaNameToHandlerMappings: List[LambdaNameToHandlerMapping],
                                      roleARN: Option[ARN],
                                      vpcConfigLookup: Option[VpcConfigLookup],
                                      timeout: FiniteDuration,
                                      memorySize: MemorySize,
                                      deadLetterARN: Option[ARN],
                                      jarStagingBucketName: BucketName,
                                      jarStagingS3KeyPrefix: S3KeyPrefix,
                                      codeFile: TaskKey[File])

object PublishLambdaConfigurationBuilder {
  import scala.language.implicitConversions

  abstract class UNDEFINED_name
  abstract class UNDEFINED_handler
  abstract class UNDEFINED_timeout
  abstract class UNDEFINED_memorySize
  abstract class UNDEFINED_stagingBucketName
  abstract class UNDEFINED_codeFile

  implicit def getPublishLambdaConfiguration(
      builder: PublishLambdaConfigurationBuilder[DEFINED, DEFINED, DEFINED, DEFINED, DEFINED, DEFINED]
  ): PublishLambdaConfiguration = {

    val nameAndHandlers: List[LambdaNameToHandlerMapping] =
      builder.names.zip(builder.handlers).map { tup =>
        LambdaNameToHandlerMapping(tup._1, tup._2)
      }

    PublishLambdaConfiguration(
      nameAndHandlers,
      builder.roleARN,
      builder.vpcConfigDeclaration,
      builder.timeout.get,
      builder.memorySize.get,
      builder.deadLetterARN,
      builder.stagingBucketName.get,
      builder.stagingBucketKeyPrefix.getOrElse(S3KeyPrefix("")),
      builder.codeFile.get
    )
  }

  def apply() =
    new PublishLambdaConfigurationBuilder[
      UNDEFINED_name,
      UNDEFINED_handler,
      UNDEFINED_timeout,
      UNDEFINED_memorySize,
      UNDEFINED_stagingBucketName,
      UNDEFINED_codeFile
    ](None, None, None, Nil, Nil, None, None, None, None, None, None)
}

class PublishLambdaConfigurationBuilder[B_LAMBDA_NAMES,
                                        B_LAMBDA_HANDLERS,
                                        B_TIMEOUT,
                                        B_MEMORY_SIZE,
                                        B_STAGING_BUCKET_NAME,
                                        B_CODE_FILE](
    val roleARN: Option[ARN],
    val vpcConfigDeclaration: Option[VpcConfigLookup],
    val nameAndHandlerType: Option[NameAndHandlerType],
    val names: List[LambdaName],
    val handlers: List[LambdaHandler],
    val timeout: Option[FiniteDuration],
    val memorySize: Option[MemorySize],
    val stagingBucketName: Option[BucketName],
    val stagingBucketKeyPrefix: Option[S3KeyPrefix],
    val deadLetterARN: Option[ARN],
    val codeFile: Option[TaskKey[File]]
) {
  def withRoleARN(arn: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      Option(ARN(arn)),
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withVpc(vpcConfigDeclaration: VpcConfigLookup) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      Option(vpcConfigDeclaration),
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withName(name: String) = {
    nameAndHandlerType match {
      case Some(MultipleNamesAndHandlers) =>
        throw new IllegalArgumentException("Cannot use withName if already used withNamesToHandlers")
      case _ => ()
    }

    new PublishLambdaConfigurationBuilder[
      DEFINED,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      Some(SingleNameAndHandler),
      List(LambdaName(name)),
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )
  }

  def withHandler(handler: String) = {
    nameAndHandlerType match {
      case Some(MultipleNamesAndHandlers) =>
        throw new IllegalArgumentException("Cannot use withHandler if already used withNamesToHandlers")
      case _ => ()
    }

    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      DEFINED,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      Some(SingleNameAndHandler),
      names,
      List(LambdaHandler(handler)),
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )
  }

  def withNamesToHandlers(namesToHandlers: (String, String)*) = {
    nameAndHandlerType match {
      case Some(SingleNameAndHandler) =>
        throw new IllegalArgumentException(
          "Cannot use withNamesToHandlers if already used withName/withHandler"
        )
      case _ => ()
    }

    val lambdaNames    = namesToHandlers.toList.map(tup => LambdaName(tup._1))
    val lambdaHandlers = namesToHandlers.toList.map(tup => LambdaHandler(tup._2))

    new PublishLambdaConfigurationBuilder[
      DEFINED,
      DEFINED,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      Some(SingleNameAndHandler),
      lambdaNames,
      lambdaHandlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )
  }

  def withTimeout(timeout: String) = {
    val parsedDuration: FiniteDuration = Duration(timeout).asInstanceOf[FiniteDuration]
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      DEFINED,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
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
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      DEFINED,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      Option(MemorySize(memorySize)),
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withStagingBucketName(name: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      DEFINED,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      Option(BucketName(name)),
      stagingBucketKeyPrefix,
      deadLetterARN,
      codeFile
    )

  def withStagingBucketKeyPrefix(name: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      Option(S3KeyPrefix(name)),
      deadLetterARN,
      codeFile
    )

  def withDeadLetterARN(arn: String) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      B_CODE_FILE
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      Option(ARN(arn)),
      codeFile
    )

  def withCodeFile(codeFileTask: TaskKey[File]) =
    new PublishLambdaConfigurationBuilder[
      B_LAMBDA_NAMES,
      B_LAMBDA_HANDLERS,
      B_TIMEOUT,
      B_MEMORY_SIZE,
      B_STAGING_BUCKET_NAME,
      DEFINED
    ](
      roleARN,
      vpcConfigDeclaration,
      nameAndHandlerType,
      names,
      handlers,
      timeout,
      memorySize,
      stagingBucketName,
      stagingBucketKeyPrefix,
      deadLetterARN,
      Option(codeFileTask)
    )
}
