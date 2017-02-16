package com.itv.chuckwagon.deploy

import cats.arrow.FunctionK
import cats.{Id, ~>}
import com.amazonaws.services.lambda.AWSLambda
import com.itv.aws.lambda._
import com.itv.aws.s3.S3Location

class AWSCompiler(val awsLambda: AWSLambda) {

  val createAlias = new AWSCreateAlias(awsLambda)
  val createLambda = new AWSCreateLambda(awsLambda)
  val deleteAlias = new AWSDeleteAlias(awsLambda)
  val deleteLambdaVersion = new AWSDeleteLambdaVersion(awsLambda)
  val listAliases = new AWSListAliases(awsLambda)
  val listPublishedLambdasWithName = new AWSListPublishedLambdasWithName(awsLambda)
  val updateAlias = new AWSUpdateAlias(awsLambda)
  val updateLambdaCode = new AWSUpdateLambdaCode(awsLambda)
  val updateLambdaConfiguration = new AWSUpdateLambdaConfiguration(awsLambda)

  def compiler: DeployLambdaA ~> Id = {
    new (DeployLambdaA ~> Id) {
      def apply[A](command: DeployLambdaA[A]): Id[A] = command match {
        case CreateAlias(publishedLambda: PublishedLambda, aliasName:AliasName) =>
          createAlias(CreateAliasRequest(publishedLambda, aliasName)).aliasedLambda
        case CreateLambda(lambda: Lambda, s3Location: S3Location) =>
          createLambda(CreateLambdaRequest(lambda, s3Location)).publishedLambda
        case DeleteAlias(lambdaName: LambdaName, alias: Alias) => {
          deleteAlias(DeleteAliasRequest(lambdaName, alias))
          ()
        }
        case DeleteLambdaVersion(publishedLambda: PublishedLambda) => {
          deleteLambdaVersion(DeleteLambdaVersionRequest(publishedLambda))
          ()
        }
        case ListAliases(lambdaName: LambdaName) =>
          listAliases(ListAliasesRequest(lambdaName)).aliases
        case ListPublishedLambdasWithName(lambdaName: LambdaName) =>
          listPublishedLambdasWithName(ListPublishedLambdasWithNameRequest(lambdaName)).publishedLambdas
        case UpdateAlias(publishedLambda: PublishedLambda, aliasName:AliasName) =>
          updateAlias(UpdateAliasRequest(publishedLambda, aliasName)).aliasedLambda
        case UpdateLambdaCode(lambda: Lambda, s3Location: S3Location) =>
          updateLambdaCode(UpdateLambdaCodeRequest(lambda, s3Location)).publishedLambda
        case UpdateLambdaConfiguration(lambda: Lambda) => {
          updateLambdaConfiguration(UpdateLambdaConfigurationRequest(lambda))
          ()
        }
      }
    }
  }

}
