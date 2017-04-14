package com.itv.chuckwagon.deploy

import cats.~>
import cats.Id
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.itv.aws.ec2._
import com.itv.aws.events._
import com.itv.aws.iam.AWSPutRolePolicy
import com.itv.aws.iam._
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.aws.sts._

class AWSCompiler(region: Regions, credentials: AWSCredentialsProvider) {

  val awsEc2    = ec2(region)(credentials)
  val awsEvents = events(region)(credentials)
  val awsIam    = iam(region)(credentials)
  val awsLambda = lambda(region)(credentials)
  val awsS3     = s3(region)(credentials)
  val awsSTS    = sts(region)(credentials)

  val findSecurityGroups = new AWSFindSecurityGroups(awsEc2)
  val findSubnets        = new AWSFindSubnets(awsEc2)
  val findVpc            = new AWSFindVpc(awsEc2)

  val putRule       = new AWSPutRule(awsEvents)
  val putTargets    = new AWSPutTargets(awsEvents)
  val deleteRule    = new AWSDeleteRule(awsEvents)
  val removeTargets = new AWSRemoveTargets(awsEvents)

  val createRole    = new AWSCreateRole(awsIam)
  val listRoles     = new AWSListRoles(awsIam)
  val putRolePolicy = new AWSPutRolePolicy(awsIam)

  val addPermission         = new AWSAddPermission(awsLambda)
  val createAlias           = new AWSCreateAlias(awsLambda)
  val createPublishedLambda = new AWSCreatePublishedLambda(awsLambda)
  val createLambdaSnapshot  = new AWSCreateLambdaSnapshot(awsLambda)
  val deleteAlias           = new AWSDeleteAlias(awsLambda)
  val deleteLambdaVersion   = new AWSDeleteLambdaVersion(awsLambda)
  val listAliases           = new AWSListAliases(awsLambda)
  val listPermissions       = new AWSListPermissions(awsLambda)
  val listPublishedLambdasWithName = new AWSListPublishedLambdasWithName(
    awsLambda
  )
  val removePermission            = new AWSRemovePermission(awsLambda)
  val updateAlias                 = new AWSUpdateAlias(awsLambda)
  val updateCodeForLambdaSnapshot = new AWSUpdateCodeForLambdaSnapshot(awsLambda)
  val updateCodeAndPublishLambda  = new AWSUpdateCodeAndPublishLambda(awsLambda)
  val updateLambdaConfiguration   = new AWSUpdateLambdaConfiguration(awsLambda)
  val getLambdaVersion            = new AWSGetLambdaVersion(awsLambda)
  val invokeLambda                = new AWSInvokeLambda(awsLambda)

  val createBucket = new AWSCreateBucket(awsS3)
  val listBuckets  = new AWSListBuckets(awsS3)
  val putObject    = new AWSPutObject(awsS3)

  val assumeRole = new AWSAssumeRole(awsSTS)

  def compiler: DeployLambdaA ~> Id =
    new (DeployLambdaA ~> Id) {
      def apply[A](command: DeployLambdaA[A]): Id[A] = command match {
        case FindSecurityGroupsUsingFilters(vpc, filters) =>
          findSecurityGroups.usingFilters(vpc, filters)
        case FindSecurityGroupsUsingIds(vpc, ids) =>
          findSecurityGroups.usingIds(vpc, ids)
        case FindSubnetsUsingFilters(vpc, filters) =>
          findSubnets.usingFilters(vpc, filters)
        case FindSubnetsUsingIds(vpc, ids) =>
          findSubnets.usingIds(vpc, ids)
        case FindVpcUsingFilters(filters) => findVpc.usingFilters(filters)
        case FindVpcUsingId(vpcId)        => findVpc.usingId(vpcId)
        case PutRule(eventRule)           => putRule(eventRule)
        case PutTargets(eventRule: EventRule, targetARN: ARN) => {
          putTargets(eventRule, targetARN)
        }
        case DeleteRule(ruleName) => {
          deleteRule(ruleName)
        }
        case RemoveTargets(ruleName) => {
          removeTargets(ruleName)
        }
        case AddPermission(alias, lambdaPermission) => {
          addPermission(alias, lambdaPermission)
        }
        case CreateAlias(
            name: AliasName,
            lambdaName: LambdaName,
            lambdaVersionToAlias: LambdaVersion
            ) =>
          createAlias(name, lambdaName, lambdaVersionToAlias)
        case CreateLambdaSnapshot(lambda: Lambda, s3Location: S3Location) =>
          createLambdaSnapshot(lambda, s3Location)
        case CreatePublishedLambda(lambda: Lambda, s3Location: S3Location) =>
          createPublishedLambda(lambda, s3Location)
        case DeleteAlias(alias: Alias) =>
          deleteAlias(alias)
        case DeleteLambdaVersion(publishedLambda: PublishedLambda) =>
          deleteLambdaVersion(publishedLambda)
        case GetLambdaVersion(lambdaName, aliasName) =>
          getLambdaVersion(lambdaName, aliasName)
        case ListAliases(lambdaName: LambdaName) =>
          listAliases(lambdaName)
        case ListPermissions(alias) =>
          listPermissions(alias)
        case ListPublishedLambdasWithName(lambdaName: LambdaName) =>
          listPublishedLambdasWithName(lambdaName)
        case RemovePermission(alias, lambdaPermission) => {
          removePermission(alias, lambdaPermission)
        }
        case UpdateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion) =>
          updateAlias(alias, lambdaVersionToAlias)
        case UpdateCodeAndPublishLambda(lambda: Lambda, s3Location: S3Location) =>
          updateCodeAndPublishLambda(lambda, s3Location)
        case UpdateCodeForLambdaSnapshot(lambda: Lambda, s3Location: S3Location) =>
          updateCodeForLambdaSnapshot(lambda, s3Location)
        case UpdateLambdaConfiguration(lambda: Lambda) => {
          updateLambdaConfiguration(lambda)
        }
        case InvokeLambda(lambdaName: LambdaName, qualifier: Option[InvokeQualifier]) =>
          invokeLambda(lambdaName, qualifier)
        case ListBuckets() =>
          listBuckets()
        case CreateBucket(name: BucketName) =>
          createBucket(name)
        case PutObject(bucket: Bucket, putObjectType: PutObjectType) =>
          putObject(bucket, putObjectType)
        case CreateRole(roleDeclaration) =>
          createRole(roleDeclaration)
        case PutRolePolicy(rolePolicy) => {
          putRolePolicy(rolePolicy)
        }
        case ListRoles() =>
          listRoles()
        case AssumeRole(roleARN, sessionName) => {
          assumeRole(roleARN, sessionName)
        }
      }
    }

}
