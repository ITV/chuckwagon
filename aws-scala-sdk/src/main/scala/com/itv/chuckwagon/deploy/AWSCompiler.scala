package com.itv.chuckwagon.deploy

import java.io.{File, InputStream}

import cats.{~>, Id}
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.s3.AmazonS3
import com.itv.aws.ec2._
import com.itv.aws.events._
import com.itv.aws.iam.{AWSPutRolePolicy, _}
import com.itv.aws.lambda._
import com.itv.aws.s3._
import com.itv.aws.sts._

class AWSCompiler(region: Regions, credentials: Option[Credentials] = None) {

  val awsEc2    = ec2(region)(credentials)
  val awsEvents = events(region)(credentials)
  val awsIam    = iam(region)(credentials)
  val awsLambda = lambda(region)(credentials)
  val awsS3     = s3(region)(credentials)
  val awsSTS    = sts(region)(credentials)

  val findSecurityGroups = new AWSFindSecurityGroups(awsEc2)
  val findSubnets        = new AWSFindSubnets(awsEc2)
  val findVPC            = new AWSFindVPC(awsEc2)

  val putRule    = new AWSPutRule(awsEvents)
  val putTargets = new AWSPutTargets(awsEvents)

  val createRole    = new AWSCreateRole(awsIam)
  val listRoles     = new AWSListRoles(awsIam)
  val putRolePolicy = new AWSPutRolePolicy(awsIam)

  val addPermission       = new AWSAddPermission(awsLambda)
  val createAlias         = new AWSCreateAlias(awsLambda)
  val createLambda        = new AWSCreateLambda(awsLambda)
  val deleteAlias         = new AWSDeleteAlias(awsLambda)
  val deleteLambdaVersion = new AWSDeleteLambdaVersion(awsLambda)
  val listAliases         = new AWSListAliases(awsLambda)
  val listPermissions     = new AWSListPermissions(awsLambda)
  val listPublishedLambdasWithName = new AWSListPublishedLambdasWithName(
    awsLambda
  )
  val removePermission          = new AWSRemovePermission(awsLambda)
  val updateAlias               = new AWSUpdateAlias(awsLambda)
  val updateLambdaCode          = new AWSUpdateLambdaCode(awsLambda)
  val updateLambdaConfiguration = new AWSUpdateLambdaConfiguration(awsLambda)
  val getLambdaVersion          = new AWSGetLambdaVersion(awsLambda)

  val createBucket = new AWSCreateBucket(awsS3)
  val listBuckets  = new AWSListBuckets(awsS3)
  val putObject    = new AWSPutObject(awsS3)

  val assumeRole = new AWSAssumeRole(awsSTS)

  def compiler: DeployLambdaA ~> Id = {
    new (DeployLambdaA ~> Id) {
      def apply[A](command: DeployLambdaA[A]): Id[A] = command match {
        case FindSecurityGroups(vpc, filters) =>
          findSecurityGroups(FindSecurityGroupsRequest(vpc, filters)).securityGroups
        case FindSubnets(vpc, filters) =>
          findSubnets(FindSubnetsRequest(vpc, filters)).subnets
        case FindVPC(filters)   => findVPC(FindVPCRequest(filters)).vpc
        case PutRule(eventRule) => putRule(PutRuleRequest(eventRule)).createdEventRule
        case PutTargets(eventRule: EventRule, targetARN: ARN) => {
          putTargets(PutTargetsRequest(eventRule, targetARN))
          ()
        }
        case AddPermission(alias, lambdaPermission) => {
          addPermission(AddPermissionRequest(alias, lambdaPermission))
          ()
        }
        case CreateAlias(
            name: AliasName,
            lambdaName: LambdaName,
            lambdaVersionToAlias: LambdaVersion
            ) =>
          createAlias(
            CreateAliasRequest(name, lambdaName, lambdaVersionToAlias)
          ).aliasedLambda
        case CreateLambda(lambda: Lambda, s3Location: S3Location) =>
          createLambda(CreateLambdaRequest(lambda, s3Location)).publishedLambda
        case DeleteAlias(alias: Alias) =>
          deleteAlias(DeleteAliasRequest(alias)).name
        case DeleteLambdaVersion(publishedLambda: PublishedLambda) =>
          deleteLambdaVersion(DeleteLambdaVersionRequest(publishedLambda)).deletedVersion
        case GetLambdaVersion(lambdaName, aliasName) =>
          getLambdaVersion(GetLambdaVersionRequest(lambdaName, aliasName)).downloadablePublishedLambda
        case ListAliases(lambdaName: LambdaName) =>
          listAliases(ListAliasesRequest(lambdaName)).aliases
        case ListPermissions(alias) =>
          listPermissions(ListPermissionsRequest(alias)).permissions
        case ListPublishedLambdasWithName(lambdaName: LambdaName) =>
          listPublishedLambdasWithName(
            ListPublishedLambdasWithNameRequest(lambdaName)
          ).publishedLambdas
        case RemovePermission(alias, lambdaPermission) => {
          removePermission(RemovePermissionRequest(alias, lambdaPermission))
          ()
        }
        case UpdateAlias(alias: Alias, lambdaVersionToAlias: LambdaVersion) =>
          updateAlias(UpdateAliasRequest(alias, lambdaVersionToAlias)).alias
        case UpdateLambdaCode(lambda: Lambda, s3Location: S3Location) =>
          updateLambdaCode(UpdateLambdaCodeRequest(lambda, s3Location)).publishedLambda
        case UpdateLambdaConfiguration(lambda: Lambda) => {
          updateLambdaConfiguration(UpdateLambdaConfigurationRequest(lambda))
          ()
        }
        case ListBuckets() =>
          listBuckets(ListBucketsRequest()).buckets
        case CreateBucket(name: BucketName) =>
          createBucket(CreateBucketRequest(name)).bucket
        case PutObject(bucket: Bucket, putObjectType: PutObjectType) =>
          putObject(PutObjectRequest(bucket, putObjectType)).key
        case CreateRole(roleDeclaration) =>
          createRole(CreateRoleRequest(roleDeclaration)).role
        case PutRolePolicy(rolePolicy) => {
          putRolePolicy(PutRolePolicyRequest(rolePolicy)).role
        }
        case ListRoles() =>
          listRoles(ListRolesRequest()).roles
        case AssumeRole(roleARN, sessionName) => {
          assumeRole(AssumeRoleRequest(roleARN, sessionName)).credentials
        }
      }
    }
  }

}
