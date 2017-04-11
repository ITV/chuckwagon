package com.itv.aws.lambda

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.GetPolicyRequest
import com.itv.aws.iam.ARN
import io.circe.parser._

import scala.util.Try

case class StatementPrincipal(Service: String)
case class StatementCondition(ArnLike: StatementArnLike)
case class StatementArnLike(`AWS:SourceArn`: String)
case class Statement(Sid: String,
                     Effect: String,
                     Principal: StatementPrincipal,
                     Action: String,
                     Resource: String,
                     Condition: StatementCondition)

case class LambdaPolicy(Statement: List[Statement])

object LambdaPolicy {
  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.generic.auto._

  implicit val LambdaPolicyDecoder = deriveDecoder[LambdaPolicy]
}

class AWSListPermissions(awsLambda: AWSLambda) {

  def extractLambdaPermissions(policyString: String): List[LambdaPermission] = {

    val statements = decode[LambdaPolicy](policyString).right.get.Statement

    val permissions = statements.map { s =>
      LambdaPermission(
        statementId = PermissionStatementId(s.Sid),
        sourceARN = ARN(s.Condition.ArnLike.`AWS:SourceArn`),
        action = PermissionAction(s.Action),
        principalService = PermissionPrincipialService(s.Principal.Service),
        targetLambdaARN = ARN(s.Resource)
      )
    }

    permissions
  }

  def apply(alias: Alias): Option[List[LambdaPermission]] = {

    val getPolicyRequest = new GetPolicyRequest()
      .withFunctionName(alias.lambdaName.value)
      .withQualifier(alias.name.value)

    Try(awsLambda.getPolicy(getPolicyRequest).getPolicy).toOption.map(extractLambdaPermissions)
  }
}
