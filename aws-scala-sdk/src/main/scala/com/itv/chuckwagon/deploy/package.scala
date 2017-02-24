package com.itv.chuckwagon

import java.io.File

import com.itv.aws.{ARN, Role, RoleName}
import com.itv.aws.lambda._
import com.itv.aws.s3._
import cats.free.Free
import cats.free.Free._

import scala.collection.immutable.Seq
import cats.syntax.list._
import cats.instances.list._
import cats.syntax.traverse._
import com.itv.aws.ec2.{SecurityGroup, Subnet, Filter, VPC}
import com.itv.aws.events.{RuleName, ScheduleExpression}

package object deploy {

  sealed trait DeployLambdaA[A]

  case class FindSecurityGroups(vpc: VPC, filters: List[Filter]) extends DeployLambdaA[List[SecurityGroup]]
  case class FindSubnets(vpc: VPC, filters: List[Filter]) extends DeployLambdaA[List[Subnet]]
  case class FindVPC(filters: List[Filter]) extends DeployLambdaA[VPC]

  case class PutRule(name: RuleName, scheduleExpression: ScheduleExpression) extends DeployLambdaA[Unit]
  case class PutTargets(ruleName: RuleName, targetARN: ARN) extends DeployLambdaA[Unit]

  case class AddPermission(lambdaName: LambdaName, action: String, principal: String, sourceARN: ARN) extends DeployLambdaA[Unit]
  case class CreateAlias(name: AliasName,
                         lambdaName: LambdaName,
                         lambdaVersionToAlias: LambdaVersion)
      extends DeployLambdaA[Alias]
  case class UpdateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion)
      extends DeployLambdaA[Alias]
  case class DeleteAlias(alias: Alias) extends DeployLambdaA[AliasName]
  case class ListAliases(lambdaName: LambdaName)
      extends DeployLambdaA[Option[List[Alias]]]

  case class ListPublishedLambdasWithName(lambdaName: LambdaName)
      extends DeployLambdaA[Option[List[PublishedLambda]]]
  case class CreateLambda(lambda: Lambda, s3Location: S3Location)
      extends DeployLambdaA[PublishedLambda]
  case class UpdateLambdaConfiguration(lambda: Lambda)
      extends DeployLambdaA[Unit]
  case class UpdateLambdaCode(lambda: Lambda, s3Location: S3Location)
      extends DeployLambdaA[PublishedLambda]
  case class DeleteLambdaVersion(publishedLambda: PublishedLambda)
      extends DeployLambdaA[LambdaVersion]

  case class CreateRole(name: RoleName, policyDocument: String)
      extends DeployLambdaA[Role]
  case class ListRoles() extends DeployLambdaA[List[Role]]

  case class CreateBucket(name: BucketName) extends DeployLambdaA[Bucket]
  case class ListBuckets() extends DeployLambdaA[List[Bucket]]
  case class PutFile(bucket: Bucket, keyPrefix: S3KeyPrefix, file: File)
      extends DeployLambdaA[S3Location]

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


  def addPermission(lambdaName: LambdaName, action: String, principal: String, sourceARN: ARN): DeployLambda[Unit] = {
    liftF[DeployLambdaA, Unit](
      AddPermission(lambdaName, action, principal, sourceARN)
    )
  }
  def createAlias(name: AliasName,
                  lambdaName: LambdaName,
                  lambdaVersionToAlias: LambdaVersion): DeployLambda[Alias] =
    liftF[DeployLambdaA, Alias](
      CreateAlias(name, lambdaName, lambdaVersionToAlias)
    )
  def updateAlias(alias: Alias,
                  lambdaVersionToAlias: LambdaVersion): DeployLambda[Alias] =
    liftF[DeployLambdaA, Alias](UpdateAlias(alias, lambdaVersionToAlias))
  def deleteAlias(alias: Alias): DeployLambda[AliasName] =
    liftF[DeployLambdaA, AliasName](DeleteAlias(alias))
  def listAliases(lambdaName: LambdaName): DeployLambda[Option[List[Alias]]] =
    liftF[DeployLambdaA, Option[List[Alias]]](ListAliases(lambdaName))

  def listPublishedLambdasWithName(
    lambdaName: LambdaName
  ): DeployLambda[Option[List[PublishedLambda]]] =
    liftF[DeployLambdaA, Option[List[PublishedLambda]]](
      ListPublishedLambdasWithName(lambdaName)
    )
  def createLambda(lambda: Lambda,
                   s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](CreateLambda(lambda, s3Location))
  def updateLambdaConfiguration(lambda: Lambda): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](UpdateLambdaConfiguration(lambda))
  def updateLambdaCode(lambda: Lambda,
                       s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](UpdateLambdaCode(lambda, s3Location))
  def deleteLambdaVersion(
    publishedLambda: PublishedLambda
  ): DeployLambda[LambdaVersion] =
    liftF[DeployLambdaA, LambdaVersion](DeleteLambdaVersion(publishedLambda))

  def createRole(name: RoleName, policyDocument: String): DeployLambda[Role] =
    liftF[DeployLambdaA, Role](CreateRole(name, policyDocument))
  def listRoles(): DeployLambda[List[Role]] =
    liftF[DeployLambdaA, List[Role]](ListRoles())

  def createBucket(name: BucketName): DeployLambda[Bucket] =
    liftF[DeployLambdaA, Bucket](CreateBucket(name))
  def listBuckets(): DeployLambda[List[Bucket]] =
    liftF[DeployLambdaA, List[Bucket]](ListBuckets())
  def putFile(bucket: Bucket,
              keyPrefix: S3KeyPrefix,
              file: File): DeployLambda[S3Location] =
    liftF[DeployLambdaA, S3Location](PutFile(bucket, keyPrefix, file))

  def getVpcConfig(maybeVpcConfigDeclaration: Option[VpcConfigDeclaration]):DeployLambda[Option[VpcConfig]] = maybeVpcConfigDeclaration match {
    case Some(vpcConfigDeclaration) => for {
      vpc <- findVPC(vpcConfigDeclaration.vpcLookupFilters)
      subnets <- findSubnets(vpc, vpcConfigDeclaration.subnetsLookupFilters)
      securityGroups <- findSecurityGroups(
        vpc,
        vpcConfigDeclaration.securityGroupsLookupFilters
      )
    } yield {
      Some(VpcConfig(vpc, subnets, securityGroups))
    }
    case None => pure[DeployLambdaA, Option[VpcConfig]](Option.empty[VpcConfig])
  }

  def publishLambda(lambda: Lambda,
                    s3Address: S3Address,
                    file: File): DeployLambda[PublishedLambda] = {
    for {
      buckets <- listBuckets()
      uploadBucketOrEmpty = buckets.find(
        b => b.name.value == s3Address.bucketName.value
      )
      uploadBucket <- uploadBucketOrEmpty match {
        case Some(bucket) => pure[DeployLambdaA, Bucket](bucket)
        case None => createBucket(s3Address.bucketName)
      }
      s3Location <- putFile(uploadBucket, s3Address.keyPrefix, file)
      alreadyPublishedLambdas <- listPublishedLambdasWithName(lambda.name)
      publishedLambda <- if (alreadyPublishedLambdas.isEmpty) {
        createLambda(lambda, s3Location)
      } else
        updateLambdaConfiguration(lambda).flatMap(
          _ => updateLambdaCode(lambda, s3Location)
        )
    } yield publishedLambda
  }

  def aliasPublishedLambda(publishedLambda: PublishedLambda,
                           aliasName: AliasName): DeployLambda[Alias] =
    for {
      alias <- aliasLambdaVersion(
        publishedLambda.lambda.name,
        publishedLambda.version,
        aliasName
      )
    } yield alias

  def aliasLambdaVersion(lambdaName: LambdaName,
                         lambdaVersion: LambdaVersion,
                         aliasName: AliasName): DeployLambda[Alias] =
    for {
      alreadyCreatedAliases <- listAliases(lambdaName)
      alias <- alreadyCreatedAliases.toList.flatten
        .find(a => a.name == aliasName) match {
        case Some(alreadyCreatedAlias) =>
          updateAlias(alreadyCreatedAlias, lambdaVersion)
        case None => createAlias(aliasName, lambdaName, lambdaVersion)
      }
    } yield alias

  def promoteLambda(lambdaName: LambdaName,
                    fromName: AliasName,
                    to: AliasName): DeployLambda[Alias] =
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
      publishedLambdas <- listPublishedLambdasWithName(lambdaName)
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
}
