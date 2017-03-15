package com.itv.chuckwagon

import java.io.{File, InputStream}

import com.itv.aws.iam.{ARN, Role, RoleName}
import com.itv.aws.lambda._
import com.itv.aws.s3._
import cats.free.Free
import cats.free.Free._
import cats.syntax.list._
import cats.instances.list._
import cats.syntax.traverse._
import com.itv.aws.Credentials
import com.itv.aws.ec2.{Filter, SecurityGroup, Subnet, VPC}
import com.itv.aws.events._
import com.itv.aws.iam.{PutRolePolicyRequest, RoleDeclaration, RolePolicy}
import com.itv.aws.sts.AssumeRoleSessionName

import scala.annotation.tailrec

package deploy {

  import java.io.InputStream

  import com.itv.aws.Credentials
  import com.itv.aws.sts.AssumeRoleSessionName

  sealed trait DeployLambdaA[A]

  case class FindSecurityGroups(vpc: VPC, filters: List[Filter]) extends DeployLambdaA[List[SecurityGroup]]
  case class FindSubnets(vpc: VPC, filters: List[Filter])        extends DeployLambdaA[List[Subnet]]
  case class FindVPC(filters: List[Filter])                      extends DeployLambdaA[VPC]

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

  case class ListPublishedLambdasWithName(lambdaName: LambdaName)               extends DeployLambdaA[Option[List[PublishedLambda]]]
  case class RemovePermission(alias: Alias, lambdaPermission: LambdaPermission) extends DeployLambdaA[Unit]
  case class CreateLambda(lambda: Lambda, s3Location: S3Location)               extends DeployLambdaA[PublishedLambda]
  case class UpdateLambdaConfiguration(lambda: Lambda)                          extends DeployLambdaA[Unit]
  case class UpdateLambdaCode(lambda: Lambda, s3Location: S3Location)           extends DeployLambdaA[PublishedLambda]
  case class DeleteLambdaVersion(publishedLambda: PublishedLambda)              extends DeployLambdaA[LambdaVersion]
  case class GetLambdaVersion(lambdaName: LambdaName, aliasName: AliasName)
      extends DeployLambdaA[DownloadablePublishedLambda]

  case class CreateRole(roleDeclaration: RoleDeclaration) extends DeployLambdaA[Role]
  case class PutRolePolicy(rolePolicy: RolePolicy)        extends DeployLambdaA[Role]
  case class ListRoles()                                  extends DeployLambdaA[List[Role]]

  case class CreateBucket(name: BucketName)                          extends DeployLambdaA[Bucket]
  case class ListBuckets()                                           extends DeployLambdaA[List[Bucket]]
  case class PutObject(bucket: Bucket, putObjectType: PutObjectType) extends DeployLambdaA[S3Location]

  case class AssumeRole(roleARN: ARN, sessionName: AssumeRoleSessionName) extends DeployLambdaA[Credentials]

}

package object deploy {

  type DeployLambda[A] = Free[DeployLambdaA, A]

  def findSecurityGroups(vpc: VPC, filters: List[Filter]): DeployLambda[List[SecurityGroup]] = {
    liftF[DeployLambdaA, List[SecurityGroup]](
      FindSecurityGroups(vpc, filters)
    )
  }
  def findSubnets(vpc: VPC, filters: List[Filter]): DeployLambda[List[Subnet]] = {
    liftF[DeployLambdaA, List[Subnet]](
      FindSubnets(vpc, filters)
    )
  }
  def findVPC(filters: List[Filter]): DeployLambda[VPC] = {
    liftF[DeployLambdaA, VPC](
      FindVPC(filters)
    )
  }

  def putRule(eventRule: EventRule): DeployLambda[CreatedEventRule] = {
    liftF[DeployLambdaA, CreatedEventRule](
      PutRule(eventRule)
    )
  }
  def putTargets(eventRule: EventRule, targetARN: ARN): DeployLambda[Unit] = {
    liftF[DeployLambdaA, Unit](
      PutTargets(eventRule, targetARN)
    )
  }
  def deleteRule(ruleName: RuleName): DeployLambda[Unit] = {
    liftF[DeployLambdaA, Unit](DeleteRule(ruleName))
  }
  def removeTargets(ruleName: RuleName): DeployLambda[Unit] = {
    liftF[DeployLambdaA, Unit](RemoveTargets(ruleName))
  }

  def addPermission(alias: Alias, lambdaPermission: LambdaPermission): DeployLambda[Unit] = {
    liftF[DeployLambdaA, Unit](
      AddPermission(alias, lambdaPermission)
    )
  }
  def createAlias(name: AliasName, lambdaName: LambdaName, lambdaVersionToAlias: LambdaVersion): DeployLambda[Alias] =
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
  def createLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](CreateLambda(lambda, s3Location))
  def updateLambdaConfiguration(lambda: Lambda): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](UpdateLambdaConfiguration(lambda))
  def updateLambdaCode(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](UpdateLambdaCode(lambda, s3Location))
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

  def assumeRole(roleARN: ARN, sessionName: AssumeRoleSessionName): DeployLambda[Credentials] =
    liftF[DeployLambdaA, Credentials](AssumeRole(roleARN, sessionName))

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

  def getOrCreateChuckwagonRole(lambdaName: LambdaName): DeployLambda[Role] =
    for {
      maybeRole <- findRole(_.roleDeclaration.name == LambdaRoles.roleNameFor(lambdaName))
      roleWithoutPolicyDoc <- maybeRole match {
        case Some(role) => pure[DeployLambdaA, Role](role)
        case None       => createRole(LambdaRoles.roleDeclarationFor(lambdaName))
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

  def getPredefinedOrChuckwagonRole(predefinedRoleARN: Option[ARN], lambdaName: LambdaName): DeployLambda[Role] =
    for {
      role <- predefinedRoleARN match {
        case Some(arn) => getPredefinedRoleOrError(arn)
        case None      => getOrCreateChuckwagonRole(lambdaName)
      }
    } yield role

  def getVpcConfig(vpcConfigDeclaration: VpcConfigDeclaration): DeployLambda[Option[VpcConfig]] =
    for {
      vpc     <- findVPC(vpcConfigDeclaration.vpcLookupFilters)
      subnets <- findSubnets(vpc, vpcConfigDeclaration.subnetsLookupFilters)
      securityGroups <- findSecurityGroups(
        vpc,
        vpcConfigDeclaration.securityGroupsLookupFilters
      )
    } yield {
      Some(VpcConfig(vpc, subnets, securityGroups))
    }

  def publishLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    for {
      alreadyPublishedLambdas <- listPublishedLambdasWithName(lambda.deployment.name)
      publishedLambda <- if (alreadyPublishedLambdas.isEmpty) {
        createLambda(lambda, s3Location)
      } else
        updateLambdaConfiguration(lambda).flatMap(
          _ => updateLambdaCode(lambda, s3Location)
        )
    } yield publishedLambda

  def uploadAndPublishLambdaToAlias(lambda: Lambda,
                                    bucketName: BucketName,
                                    putObjectType: PutObjectType,
                                    aliasName: AliasName): DeployLambda[Alias] = {
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
      publishedLambda <- publishLambda(lambda, s3Location)
      alias <- aliasLambdaVersion(
        publishedLambda.lambda.deployment.name,
        publishedLambda.version,
        aliasName
      )
    } yield alias
  }

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
          description = s"Periodic Trigger for Lambda '${alias.lambdaName.value}' in environment '${alias.name.value}'"
        ))
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

  def getPublishedLambdaForAliasName(lambdaName: LambdaName, aliasName: AliasName): DeployLambda[PublishedLambda] =
    for {
      maybeAlias <- findAlias(lambdaName, aliasName)
      alias = maybeAlias.getOrElse(
        throw new Exception(
          s"There should be an alias '${aliasName.value}' for '${lambdaName.value}' but there was not."))
      publishedLambdas <- listPublishedLambdasWithName(alias.lambdaName)
      publishedLambda = publishedLambdas.getOrElse(Nil).find(_.version == alias.lambdaVersion)
    } yield
      publishedLambda match {
        case Some(pl) => pl
        case None =>
          throw new Exception(
            s"There should be a published Lambda for '${alias.name.value}' with version '${alias.lambdaVersion.value}' but there wasn't")
      }
}
