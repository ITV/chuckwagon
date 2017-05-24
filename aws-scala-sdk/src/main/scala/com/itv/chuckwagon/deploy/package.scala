package com.itv.chuckwagon

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

import com.itv.aws.iam.ARN
import com.itv.aws.iam.Role
import com.itv.aws.iam.RoleName
import com.itv.aws.lambda._
import com.itv.aws.s3._
import cats.free.Free
import cats.free.Free._
import cats.syntax.list._
import cats.instances.list._
import cats.syntax.traverse._
import com.itv.aws.StaticCredentialsProvider
import com.itv.aws.ec2.Filter
import com.itv.aws.ec2.SecurityGroup
import com.itv.aws.ec2.Subnet
import com.itv.aws.ec2.Vpc
import com.itv.aws.events._
import com.itv.aws.iam.RoleDeclaration
import com.itv.aws.iam.RolePolicy
import com.itv.aws.sts.AssumeRoleSessionName

import scala.annotation.tailrec

package deploy {

  import java.io.InputStream

  import com.itv.aws.StaticCredentialsProvider
  import com.itv.aws.ec2.SecurityGroupId
  import com.itv.aws.ec2.SubnetId
  import com.itv.aws.ec2.VpcId
  import com.itv.aws.sts.AssumeRoleSessionName

  sealed trait DeployLambdaA[A]

  case class FindSecurityGroupsUsingFilters(vpc: Vpc, filters: List[Filter])
      extends DeployLambdaA[List[SecurityGroup]]
  case class FindSecurityGroupsUsingIds(vpc: Vpc, ids: List[SecurityGroupId])
      extends DeployLambdaA[List[SecurityGroup]]
  case class FindSubnetsUsingIds(vpc: Vpc, ids: List[SubnetId])       extends DeployLambdaA[List[Subnet]]
  case class FindSubnetsUsingFilters(vpc: Vpc, filters: List[Filter]) extends DeployLambdaA[List[Subnet]]
  case class FindVpcUsingId(id: VpcId)                                extends DeployLambdaA[Vpc]
  case class FindVpcUsingFilters(filters: List[Filter])               extends DeployLambdaA[Vpc]

  case class PutRule(eventRule: EventRule)                    extends DeployLambdaA[CreatedEventRule]
  case class PutTargets(eventRule: EventRule, targetARN: ARN) extends DeployLambdaA[Unit]
  case class DeleteRule(ruleName: RuleName)                   extends DeployLambdaA[Unit]
  case class RemoveTargets(ruleName: RuleName)                extends DeployLambdaA[Unit]

  case class AddPermission(alias: Alias, lambdaPermission: LambdaPermission) extends DeployLambdaA[Unit]
  case class CreateAlias(name: AliasName, lambdaName: LambdaName, lambdaVersionToAlias: LambdaVersion)
      extends DeployLambdaA[Alias]
  case class UpdateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion) extends DeployLambdaA[Alias]
  case class DeleteAlias(alias: Alias)                                      extends DeployLambdaA[AliasName]
  case class ListAliases(lambdaName: LambdaName)                            extends DeployLambdaA[Option[List[Alias]]]
  case class ListPermissions(alias: Alias)                                  extends DeployLambdaA[Option[List[LambdaPermission]]]

  case class ListPublishedLambdasWithName(lambdaName: LambdaName)
      extends DeployLambdaA[Option[List[PublishedLambda]]]
  case class RemovePermission(alias: Alias, lambdaPermission: LambdaPermission) extends DeployLambdaA[Unit]
  case class CreateLambdaSnapshot(lambda: Lambda, s3Location: S3Location)       extends DeployLambdaA[LambdaSnapshot]
  case class CreatePublishedLambda(lambda: Lambda, s3Location: S3Location)
      extends DeployLambdaA[PublishedLambda]

  case class UpdateLambdaConfiguration(lambda: Lambda) extends DeployLambdaA[Unit]
  case class UpdateCodeForLambdaSnapshot(lambda: Lambda, s3Location: S3Location)
      extends DeployLambdaA[LambdaSnapshot]
  case class UpdateCodeAndPublishLambda(lambda: Lambda, s3Location: S3Location)
      extends DeployLambdaA[PublishedLambda]
  case class DeleteLambdaVersion(publishedLambda: PublishedLambda) extends DeployLambdaA[LambdaVersion]
  case class GetLambdaVersion(lambdaName: LambdaName, aliasName: AliasName)
      extends DeployLambdaA[DownloadablePublishedLambda]
  case class InvokeLambda(lambdaName: LambdaName, qualifier: Option[InvokeQualifier], payload: Option[String])
      extends DeployLambdaA[LambdaResponse]

  case class CreateRole(roleDeclaration: RoleDeclaration) extends DeployLambdaA[Role]
  case class PutRolePolicy(rolePolicy: RolePolicy)        extends DeployLambdaA[Role]
  case class ListRoles()                                  extends DeployLambdaA[List[Role]]

  case class CreateBucket(name: BucketName)                          extends DeployLambdaA[Bucket]
  case class ListBuckets()                                           extends DeployLambdaA[List[Bucket]]
  case class PutObject(bucket: Bucket, putObjectType: PutObjectType) extends DeployLambdaA[S3Location]

  case class AssumeRole(roleARN: ARN, sessionName: AssumeRoleSessionName)
      extends DeployLambdaA[StaticCredentialsProvider]

}

package object deploy {

  type DeployLambda[A] = Free[DeployLambdaA, A]

  def putRule(eventRule: EventRule): DeployLambda[CreatedEventRule] =
    liftF[DeployLambdaA, CreatedEventRule](
      PutRule(eventRule)
    )
  def putTargets(eventRule: EventRule, targetARN: ARN): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](
      PutTargets(eventRule, targetARN)
    )
  def deleteRule(ruleName: RuleName): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](DeleteRule(ruleName))
  def removeTargets(ruleName: RuleName): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](RemoveTargets(ruleName))

  def addPermission(alias: Alias, lambdaPermission: LambdaPermission): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](
      AddPermission(alias, lambdaPermission)
    )
  def createAlias(name: AliasName,
                  lambdaName: LambdaName,
                  lambdaVersionToAlias: LambdaVersion): DeployLambda[Alias] =
    liftF[DeployLambdaA, Alias](
      CreateAlias(name, lambdaName, lambdaVersionToAlias)
    )
  def updateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion): DeployLambda[Alias] =
    liftF[DeployLambdaA, Alias](UpdateAlias(alias, lambdaVersionToAlias))
  def deleteAlias(alias: Alias): DeployLambda[AliasName] =
    liftF[DeployLambdaA, AliasName](DeleteAlias(alias))
  def listAliases(lambdaName: LambdaName): DeployLambda[Option[List[Alias]]] =
    liftF[DeployLambdaA, Option[List[Alias]]](ListAliases(lambdaName))

  def listPermissions(alias: Alias): DeployLambda[Option[List[LambdaPermission]]] =
    liftF[DeployLambdaA, Option[List[LambdaPermission]]](
      ListPermissions(alias)
    )
  def listPublishedLambdasWithName(
      lambdaName: LambdaName
  ): DeployLambda[Option[List[PublishedLambda]]] =
    liftF[DeployLambdaA, Option[List[PublishedLambda]]](
      ListPublishedLambdasWithName(lambdaName)
    )
  def removePermission(alias: Alias, lambdaPermission: LambdaPermission): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](RemovePermission(alias, lambdaPermission))

  def createLambdaSnapshot(lambda: Lambda, s3Location: S3Location): DeployLambda[LambdaSnapshot] =
    liftF[DeployLambdaA, LambdaSnapshot](CreateLambdaSnapshot(lambda, s3Location))
  def createPublishedLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](CreatePublishedLambda(lambda, s3Location))

  def updateLambdaConfiguration(lambda: Lambda): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](UpdateLambdaConfiguration(lambda))

  def updateCodeForLambdaSnapshot(lambda: Lambda, s3Location: S3Location): DeployLambda[LambdaSnapshot] =
    liftF[DeployLambdaA, LambdaSnapshot](UpdateCodeForLambdaSnapshot(lambda, s3Location))

  def updateCodeAndPublishLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](UpdateCodeAndPublishLambda(lambda, s3Location))

  def invokeLambda(lambdaName: LambdaName,
                   qualifier: Option[InvokeQualifier],
                   payload: Option[String]): DeployLambda[LambdaResponse] =
    liftF[DeployLambdaA, LambdaResponse](InvokeLambda(lambdaName, qualifier, payload))

  def deleteLambdaVersion(
      publishedLambda: PublishedLambda
  ): DeployLambda[LambdaVersion] =
    liftF[DeployLambdaA, LambdaVersion](DeleteLambdaVersion(publishedLambda))
  def getDownloadablePublishedLambdaVersion(lambdaName: LambdaName,
                                            aliasName: AliasName): DeployLambda[DownloadablePublishedLambda] =
    liftF[DeployLambdaA, DownloadablePublishedLambda](GetLambdaVersion(lambdaName, aliasName))

  def createRole(roleDeclaration: RoleDeclaration): DeployLambda[Role] =
    liftF[DeployLambdaA, Role](CreateRole(roleDeclaration))
  def putRolePolicy(rolePolicy: RolePolicy): DeployLambda[Role] =
    liftF[DeployLambdaA, Role](PutRolePolicy(rolePolicy))
  def listRoles(): DeployLambda[List[Role]] =
    liftF[DeployLambdaA, List[Role]](ListRoles())

  def createBucket(name: BucketName): DeployLambda[Bucket] =
    liftF[DeployLambdaA, Bucket](CreateBucket(name))
  def listBuckets(): DeployLambda[List[Bucket]] =
    liftF[DeployLambdaA, List[Bucket]](ListBuckets())
  def putObject(bucket: Bucket, putObjectType: PutObjectType): DeployLambda[S3Location] =
    liftF[DeployLambdaA, S3Location](PutObject(bucket, putObjectType))

  def assumeRole(roleARN: ARN, sessionName: AssumeRoleSessionName): DeployLambda[StaticCredentialsProvider] =
    liftF[DeployLambdaA, StaticCredentialsProvider](AssumeRole(roleARN, sessionName))

  def findRole(test: Role => Boolean): DeployLambda[Option[Role]] =
    for {
      roles <- listRoles()
      maybeRole <- roles.find(test) match {
        case r @ Some(role) =>
          pure[DeployLambdaA, Option[Role]](r)
        case None =>
          pure[DeployLambdaA, Option[Role]](None)
      }
    } yield maybeRole

  def getOrCreateChuckwagonRole(roleName: RoleName): DeployLambda[Role] =
    for {
      maybeRole <- findRole(_.roleDeclaration.name == roleName)
      roleWithoutPolicyDoc <- maybeRole match {
        case Some(role) => pure[DeployLambdaA, Role](role)
        case None       => createRole(LambdaRoles.roleDeclarationFor(roleName))
      }
      role <- putRolePolicy(LambdaRoles.rolePolicyFor(roleWithoutPolicyDoc))
    } yield role

  def getPredefinedRoleOrError(predefinedRoleARN: ARN): DeployLambda[Role] =
    for {
      maybeRole <- findRole(_.arn == predefinedRoleARN)
      role <- maybeRole match {
        case Some(r) => pure[DeployLambdaA, Role](r)
        case None =>
          throw new Exception(s"Predefined Role '${predefinedRoleARN.value}' doesn't exist")
      }
    } yield role

  def getPredefinedOrChuckwagonRole(predefinedRoleARN: Option[ARN], roleName: RoleName): DeployLambda[Role] =
    for {
      role <- predefinedRoleARN match {
        case Some(arn) => getPredefinedRoleOrError(arn)
        case None      => getOrCreateChuckwagonRole(roleName)
      }
    } yield role

  def publishLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    for {
      alreadyPublishedLambdas <- listPublishedLambdasWithName(lambda.deployment.name)
      publishedLambda <- if (alreadyPublishedLambdas.isEmpty) {
        createPublishedLambda(lambda, s3Location)
      } else
        updateLambdaConfiguration(lambda).flatMap(
          _ => updateCodeAndPublishLambda(lambda, s3Location)
        )
    } yield publishedLambda
  def publishLambdas(lambdas: List[Lambda], s3Location: S3Location): DeployLambda[List[PublishedLambda]] =
    lambdas.map { lambda =>
      publishLambda(lambda, s3Location)
    }.sequenceU

  def publishLambdaSnapshot(lambda: Lambda, s3Location: S3Location): DeployLambda[LambdaSnapshot] =
    for {
      alreadyPublishedLambdas <- listPublishedLambdasWithName(lambda.deployment.name)
      publishedLambda <- if (alreadyPublishedLambdas.isEmpty) {
        createLambdaSnapshot(lambda, s3Location)
      } else
        updateLambdaConfiguration(lambda).flatMap(
          _ => updateCodeForLambdaSnapshot(lambda, s3Location)
        )
    } yield publishedLambda
  def publishLambdaSnapshots(lambdas: List[Lambda],
                             s3Location: S3Location): DeployLambda[List[LambdaSnapshot]] =
    lambdas.map { lambda =>
      publishLambdaSnapshot(lambda, s3Location)
    }.sequenceU

  def uploadAndPublishLambdas(lambdas: List[Lambda],
                              bucketName: BucketName,
                              putObjectType: PutObjectType): DeployLambda[List[PublishedLambda]] =
    for {
      buckets <- listBuckets()
      uploadBucketOrEmpty = buckets.find(
        b => b.name.value == bucketName.value
      )
      uploadBucket <- uploadBucketOrEmpty match {
        case Some(bucket) => pure[DeployLambdaA, Bucket](bucket)
        case None         => createBucket(bucketName)
      }
      s3Location       <- putObject(uploadBucket, putObjectType)
      publishedLambdas <- publishLambdas(lambdas, s3Location)
    } yield publishedLambdas

  def uploadAndPublishLambdaSnapshots(lambdas: List[Lambda],
                                      bucketName: BucketName,
                                      putObjectType: PutObjectType): DeployLambda[List[LambdaSnapshot]] =
    for {
      buckets <- listBuckets()
      uploadBucketOrEmpty = buckets.find(
        b => b.name.value == bucketName.value
      )
      uploadBucket <- uploadBucketOrEmpty match {
        case Some(bucket) => pure[DeployLambdaA, Bucket](bucket)
        case None         => createBucket(bucketName)
      }
      s3Location      <- putObject(uploadBucket, putObjectType)
      lambdaSnapshots <- publishLambdaSnapshots(lambdas, s3Location)
    } yield lambdaSnapshots

  def aliasPublishedLambda(publishedLambda: PublishedLambda, aliasName: AliasName): DeployLambda[Alias] =
    for {
      alias <- aliasLambdaVersion(
        publishedLambda.lambda.deployment.name,
        publishedLambda.version,
        aliasName
      )
    } yield alias

  def findAlias(lambdaName: LambdaName, aliasName: AliasName): DeployLambda[Option[Alias]] =
    for {
      alreadyCreatedAliases <- listAliases(lambdaName)
      alias = alreadyCreatedAliases.toList.flatten
        .find(a => a.name == aliasName)
    } yield alias

  def aliasLambdaVersion(lambdaName: LambdaName,
                         lambdaVersion: LambdaVersion,
                         aliasName: AliasName): DeployLambda[Alias] =
    for {
      maybeAlias <- findAlias(lambdaName, aliasName)
      alias <- maybeAlias match {
        case Some(alreadyCreatedAlias) =>
          updateAlias(alreadyCreatedAlias, lambdaVersion)
        case None => createAlias(aliasName, lambdaName, lambdaVersion)
      }
    } yield alias

  def promoteLambda(lambdaName: LambdaName, fromName: AliasName, to: AliasName): DeployLambda[Alias] =
    for {
      alreadyCreatedAliases <- listAliases(lambdaName)
      alias <- alreadyCreatedAliases.toList.flatten
        .find(a => a.name == fromName) match {
        case Some(from) =>
          aliasLambdaVersion(from.lambdaName, from.lambdaVersion, to)
        case None =>
          throw new IllegalArgumentException(
            s"No such '$fromName' to promote from"
          )
      }
    } yield alias
  def promoteLambdas(lambdaNames: List[LambdaName],
                     fromName: AliasName,
                     to: AliasName): DeployLambda[List[Alias]] =
    lambdaNames.map { lambdaName =>
      promoteLambda(lambdaName, fromName, to)
    }.sequenceU

  def deleteRedundantAliases(
      lambdaName: LambdaName,
      desiredAliasNames: List[AliasName]
  ): DeployLambda[List[AliasName]] =
    for {
      alreadyCreatedAliases <- listAliases(lambdaName)
      aliasesToDelete = alreadyCreatedAliases.toList.flatten
        .filterNot(a => desiredAliasNames.toSet.contains(a.name))
      deletedAliases <- aliasesToDelete.map(deleteAlias).sequenceU
    } yield deletedAliases

  def deleteRedundantPublishedLambdas(
      lambdaName: LambdaName
  ): DeployLambda[List[LambdaVersion]] =
    for {
      publishedLambdas      <- listPublishedLambdasWithName(lambdaName)
      alreadyCreatedAliases <- listAliases(lambdaName)
      publishedLambdasToDelete: List[PublishedLambda] = publishedLambdas.toList.flatten
        .filterNot(
          pl =>
            alreadyCreatedAliases.toList.flatten
              .map(_.lambdaVersion)
              .toSet
              .contains(pl.version)
        )
      deletedLambdaVersions <- publishedLambdasToDelete
        .map(deleteLambdaVersion)
        .sequenceU
    } yield deletedLambdaVersions

  def putLambdaPermission(alias: Alias, sourcePermissionName: String, sourceARN: ARN): DeployLambda[Unit] = {
    val statementId = PermissionStatementId(s"${alias.derivedId}-$sourcePermissionName-permission")
    for {
      existingPermissions <- listPermissions(alias)
      maybeToDelete = existingPermissions.getOrElse(Nil).find(_.statementId == statementId)
      _ <- maybeToDelete match {
        case Some(lambdaPermission) => removePermission(alias, lambdaPermission)
        case None                   => pure[DeployLambdaA, Option[VpcConfig]](Option.empty[VpcConfig])
      }
      _ <- addPermission(
        alias,
        LambdaPermission(
          statementId = statementId,
          sourceARN = sourceARN,
          action = PermissionAction("lambda:InvokeFunction"),
          principalService = PermissionPrincipialService("events.amazonaws.com"),
          targetLambdaARN = alias.arn
        )
      )
    } yield ()
  }

  private val LAMBDA_SCHEDULED_TRIGGER_NAME = "scheduled-trigger"
  def ruleNameFor(alias: Alias): RuleName =
    RuleName(s"${alias.derivedId}-$LAMBDA_SCHEDULED_TRIGGER_NAME")
  def setLambdaTrigger(alias: Alias, scheduleExpression: ScheduleExpression): DeployLambda[Unit] =
    for {
      createdEventRule <- putRule(
        EventRule(
          name = ruleNameFor(alias),
          scheduleExpression = scheduleExpression,
          description =
            s"Periodic Trigger for Lambda '${alias.lambdaName.value}' in environment '${alias.name.value}'"
        )
      )
      _ <- putTargets(createdEventRule.eventRule, alias.arn)
      _ <- putLambdaPermission(alias, LAMBDA_SCHEDULED_TRIGGER_NAME, createdEventRule.arn)
    } yield ()
  def removeLambdaTrigger(alias: Alias): DeployLambda[Unit] = {
    val ruleName = ruleNameFor(alias)
    for {
      _ <- removeTargets(ruleName)
      _ <- deleteRule(ruleName)
    } yield ()
  }

  def getPublishedLambdaForAliasName(lambdaName: LambdaName,
                                     aliasName: AliasName): DeployLambda[PublishedLambda] =
    for {
      maybeAlias <- findAlias(lambdaName, aliasName)
      alias = maybeAlias.getOrElse(
        throw new Exception(
          s"There should be an alias '${aliasName.value}' for '${lambdaName.value}' but there was not."
        )
      )
      publishedLambdas <- listPublishedLambdasWithName(alias.lambdaName)
      publishedLambda = publishedLambdas.getOrElse(Nil).find(_.version == alias.lambdaVersion)
    } yield
      publishedLambda match {
        case Some(pl) => pl
        case None =>
          throw new Exception(
            s"There should be a published Lambda for '${alias.name.value}' with version '${alias.lambdaVersion.value}' but there wasn't"
          )
      }
}
