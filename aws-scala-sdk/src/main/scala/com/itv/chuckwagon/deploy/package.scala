package com.itv.chuckwagon


import java.io.File

import com.itv.aws.{Role, RoleName}
import com.itv.aws.lambda._
import com.itv.aws.s3.{Bucket, S3KeyPrefix, S3Location}
import cats.free.Free
import cats.free.Free.liftF

import scala.collection.immutable.Seq

import cats.syntax.list._
import cats.instances.list._
import cats.syntax.traverse._


package object deploy {

  sealed trait DeployLambdaA[A]

  case class CreateAlias(publishedLambda: PublishedLambda, aliasName:AliasName) extends DeployLambdaA[AliasedLambda]
  case class UpdateAlias(publishedLambda: PublishedLambda, aliasName:AliasName) extends DeployLambdaA[AliasedLambda]
  case class DeleteAlias(lambdaName: LambdaName, alias: Alias) extends DeployLambdaA[Unit]
  case class ListAliases(lambdaName: LambdaName) extends DeployLambdaA[List[Alias]]

  case class ListPublishedLambdasWithName(lambdaName: LambdaName) extends DeployLambdaA[List[PublishedLambda]]
  case class CreateLambda(lambda: Lambda,
                          s3Location: S3Location) extends DeployLambdaA[PublishedLambda]
  case class UpdateLambdaConfiguration(lambda: Lambda) extends DeployLambdaA[Unit]
  case class UpdateLambdaCode(lambda: Lambda,
                              s3Location: S3Location) extends DeployLambdaA[PublishedLambda]
  case class DeleteLambdaVersion(publishedLambda: PublishedLambda) extends DeployLambdaA[Unit]

  case class CreateRole(name: RoleName, policyDocument: String) extends DeployLambdaA[Role]
  case class ListRoles() extends DeployLambdaA[List[Role]]

  case class CreateBucket(name: String) extends DeployLambdaA[Bucket]
  case class ListBuckets() extends DeployLambdaA[List[Bucket]]
  case class PutFile(bucket: Bucket, keyPrefix: S3KeyPrefix, file:File) extends DeployLambdaA[S3Location]

  type DeployLambda[A] = Free[DeployLambdaA, A]

  def createAlias(publishedLambda: PublishedLambda, aliasName: AliasName): DeployLambda[AliasedLambda] =
    liftF[DeployLambdaA, AliasedLambda](CreateAlias(publishedLambda, aliasName))
  def updateAlias(publishedLambda: PublishedLambda, aliasName: AliasName): DeployLambda[AliasedLambda] =
    liftF[DeployLambdaA, AliasedLambda](UpdateAlias(publishedLambda, aliasName))
  def deleteAlias(lambdaName: LambdaName, alias: Alias): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](DeleteAlias(lambdaName, alias))
  def listAliases(lambdaName: LambdaName): DeployLambda[List[Alias]] =
    liftF[DeployLambdaA, List[Alias]](ListAliases(lambdaName))

  def listPublishedLambdasWithName(lambdaName: LambdaName): DeployLambda[List[PublishedLambda]] =
    liftF[DeployLambdaA, List[PublishedLambda]](ListPublishedLambdasWithName(lambdaName))
  def createLambda(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](CreateLambda(lambda, s3Location))
  def updateLambdaConfiguration(lambda: Lambda): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](UpdateLambdaConfiguration(lambda))
  def updateLambdaCode(lambda: Lambda, s3Location: S3Location): DeployLambda[PublishedLambda] =
    liftF[DeployLambdaA, PublishedLambda](UpdateLambdaCode(lambda, s3Location))
  def deleteLambdaVersion(publishedLambda: PublishedLambda): DeployLambda[Unit] =
    liftF[DeployLambdaA, Unit](DeleteLambdaVersion(publishedLambda))

  def createRole(name: RoleName, policyDocument: String): DeployLambda[Role] =
    liftF[DeployLambdaA, Role](CreateRole(name, policyDocument))
  def listRoles(): DeployLambda[List[Role]] =
    liftF[DeployLambdaA, List[Role]](ListRoles())

  def createBucket(name: String): DeployLambda[Bucket] =
    liftF[DeployLambdaA, Bucket](CreateBucket(name))
  def listBuckets(): DeployLambda[List[Bucket]] =
    liftF[DeployLambdaA, List[Bucket]](ListBuckets())
  def putFile(bucket: Bucket, keyPrefix: S3KeyPrefix, file:File): DeployLambda[S3Location] =
    liftF[DeployLambdaA, S3Location](PutFile(bucket, keyPrefix, file))

  def createOrUpdateLambda(lambda: Lambda, s3Location: S3Location, alreadyPublishedLambdas:List[PublishedLambda]): DeployLambda[PublishedLambda] = {
    if (alreadyPublishedLambdas.isEmpty) {
      createLambda(lambda, s3Location)
    } else
      updateLambdaConfiguration(lambda).flatMap(_ => updateLambdaCode(lambda, s3Location))
  }

  def promoteLambda(publishedLambda: PublishedLambda, aliasName:AliasName, alreadyCreatedAliases: List[Alias]): DeployLambda[AliasedLambda] =
    if (alreadyCreatedAliases.map(_.name).contains(aliasName)) {
      updateAlias(publishedLambda, aliasName)
    } else {
      createAlias(publishedLambda, aliasName)
    }

  def deleteRedundantAliases(lambdaName: LambdaName, desiredAliasNames: List[AliasName], actualAliases: List[Alias]): DeployLambda[Unit] = {
    val aliasesToDelete = actualAliases.filterNot(a => desiredAliasNames.toSet.contains(a.name))
    aliasesToDelete.map(deleteAlias(lambdaName, _)).sequenceU.map(_ => ())
  }

  def deleteRedundantPublishedLambdas(lambdaName: LambdaName, desiredAliases: List[Alias]): DeployLambda[Unit] = for {
    publishedLambdas <- listPublishedLambdasWithName(lambdaName)
    publishedLambdasToDelete: List[PublishedLambda] = publishedLambdas.filterNot(pl => desiredAliases.map(_.lambdaVersion).toSet.contains(pl))
    _ <- publishedLambdasToDelete.map(deleteLambdaVersion).sequenceU
  } yield ()
}
