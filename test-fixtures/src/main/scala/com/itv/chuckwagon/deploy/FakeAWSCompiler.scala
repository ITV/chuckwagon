package com.itv.chuckwagon.deploy

import cats.Id
import cats.~>
import com.itv.aws.ec2.SecurityGroup
import com.itv.aws.ec2.Subnet
import com.itv.aws.ec2.Vpc
import com.itv.aws.events.EventRule
import com.itv.aws.iam.ARN
import com.itv.aws.iam.Role
import com.itv.aws.lambda._
import com.itv.aws.s3._

case class FakeAWSCompiler(
    arnSnapshot: ARN = ARN("lambdaARN"),
    nextVersion: LambdaVersion = LambdaVersion(1),
    buckets: List[Bucket] = Nil,
    lambdaForName: (LambdaName => Option[Lambda]) = _ => None,
    commandRecorder: (DeployLambdaA[_] => Unit) = (_: DeployLambdaA[_]) => ()
) {

  def arnFor(lambdaVersion: LambdaVersion) =
    ARN(arnSnapshot.value + s":${lambdaVersion.value}")
  val key: S3Key                                     = S3Key("s3Location")
  val lambdasWithName: Option[List[PublishedLambda]] = None

  val securityGroups: List[SecurityGroup] = Nil
  val subnets: List[Subnet]               = Nil
  val vpc                                 = Vpc("")

  val roleARN = ARN("roleARN")

  def compiler: DeployLambdaA ~> Id =
    new (DeployLambdaA ~> Id) {
      def apply[A](command: DeployLambdaA[A]): Id[A] = {
        commandRecorder(command)
        command match {
          case FindSecurityGroupsUsingFilters(vpc, filters) => securityGroups
          case FindSecurityGroupsUsingIds(vpc, ids)         => securityGroups
          case FindSubnetsUsingFilters(vpc, filters)        => subnets
          case FindSubnetsUsingIds(vpc, ids)                => subnets
          case FindVpcUsingFilters(filters)                 => vpc
          case FindVpcUsingId(vpcId)                        => vpc
          case PutRule(eventRule)                           => ???
          case PutTargets(eventRule: EventRule, targetARN: ARN) =>
            ???
          case DeleteRule(ruleName)                   => ???
          case RemoveTargets(ruleName)                => ???
          case AddPermission(alias, lambdaPermission) => ???
          case CreateAlias(
              name: AliasName,
              lambdaName: LambdaName,
              lambdaVersionToAlias: LambdaVersion
              ) =>
            ???
          case CreateLambdaSnapshot(lambda: Lambda, s3Location: S3Location) =>
            LambdaSnapshot(lambda, arnSnapshot)
          case CreatePublishedLambda(lambda: Lambda, s3Location: S3Location) =>
            PublishedLambda(lambda, nextVersion, arnFor(nextVersion))
          case DeleteAlias(alias: Alias) =>
            ???
          case DeleteLambdaVersion(publishedLambda: PublishedLambda) =>
            ???
          case GetLambdaVersion(lambdaName, aliasName) =>
            ???
          case ListAliases(lambdaName: LambdaName) =>
            ???
          case ListPermissions(alias) =>
            ???
          case ListPublishedLambdasWithName(lambdaName: LambdaName) =>
            lambdaForName(lambdaName).map { lambda =>
              val version = LambdaVersion(nextVersion.value - 1)
              List(PublishedLambda(lambda, version, arnFor(version)))
            }
          case RemovePermission(alias, lambdaPermission) => ???
          case UpdateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion) =>
            ???
          case UpdateCodeAndPublishLambda(lambda: Lambda, s3Location: S3Location) =>
            PublishedLambda(lambda, nextVersion, arnFor(nextVersion))
          case UpdateCodeForLambdaSnapshot(lambda: Lambda, s3Location: S3Location) =>
            LambdaSnapshot(lambda, arnSnapshot)
          case UpdateLambdaConfiguration(lambda: Lambda)    => ()
          case InvokeLambda(lambdaName, qualifier, payload) => ???
          case ListBuckets()                                => buckets
          case CreateBucket(name: BucketName)               => Bucket(name)
          case PutObject(bucket: Bucket, putObjectType: PutObjectType) =>
            S3Location(bucket, key)
          case CreateRole(roleDeclaration)      => Role(roleDeclaration, roleARN)
          case PutRolePolicy(rolePolicy)        => ???
          case ListRoles()                      => ???
          case AssumeRole(roleARN, sessionName) => ???
        }
      }
    }
}
