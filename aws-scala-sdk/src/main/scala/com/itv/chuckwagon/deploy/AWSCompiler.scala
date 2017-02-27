package com.itv.chuckwagon.deploy

import java.io.File

import cats.arrow.FunctionK
import cats.{Id, ~>}
import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.ARN
import com.itv.aws.ec2._
import com.itv.aws.events._
import com.itv.aws.iam.{AWSPutRolePolicy, _}
import com.itv.aws.lambda._
import com.itv.aws.s3._

class AWSCompiler(val awsLambda: AWSLambda) {

  val addPermission = new AWSAddPermission(awsLambda)
  val createAlias = new AWSCreateAlias(awsLambda)
  val createLambda = new AWSCreateLambda(awsLambda)
  val deleteAlias = new AWSDeleteAlias(awsLambda)
  val deleteLambdaVersion = new AWSDeleteLambdaVersion(awsLambda)
  val listAliases = new AWSListAliases(awsLambda)
  val listPermissions = new AWSListPermissions(awsLambda)
  val listPublishedLambdasWithName = new AWSListPublishedLambdasWithName(
    awsLambda
  )
  val removePermission = new AWSRemovePermission(awsLambda)
  val updateAlias = new AWSUpdateAlias(awsLambda)
  val updateLambdaCode = new AWSUpdateLambdaCode(awsLambda)
  val updateLambdaConfiguration = new AWSUpdateLambdaConfiguration(awsLambda)

  def compiler: DeployLambdaA ~> Id = {
    new (DeployLambdaA ~> Id) {
      def apply[A](command: DeployLambdaA[A]): Id[A] = command match {
        case FindSecurityGroups(vpc, filters) =>
          AWSFindSecurityGroups(FindSecurityGroupsRequest(vpc, filters)).securityGroups
        case FindSubnets(vpc, filters) =>
          AWSFindSubnets(FindSubnetsRequest(vpc, filters)).subnets
        case FindVPC(filters) => AWSFindVPC(FindVPCRequest(filters)).vpc
        case PutRule(eventRule) => AWSPutRule(PutRuleRequest(eventRule)).createdEventRule
        case PutTargets(eventRule: EventRule, targetARN: ARN) => {
          AWSPutTargets(PutTargetsRequest(eventRule, targetARN))
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
          AWSListBuckets(ListBucketsRequest()).buckets
        case CreateBucket(name: BucketName) =>
          AWSCreateBucket(CreateBucketRequest(name)).bucket
        case PutFile(bucket: Bucket, keyPrefix: S3KeyPrefix, file: File) =>
          AWSPutFile(PutFileRequest(bucket, keyPrefix, file)).key
        case CreateRole(roleDeclaration) =>
          AWSCreateRole(CreateRoleRequest(roleDeclaration)).role
        case PutRolePolicy(rolePolicy) => {
          AWSPutRolePolicy(PutRolePolicyRequest(rolePolicy)).role
        }
        case ListRoles() =>
          AWSListRoles(ListRolesRequest()).roles
      }
    }
  }

}
