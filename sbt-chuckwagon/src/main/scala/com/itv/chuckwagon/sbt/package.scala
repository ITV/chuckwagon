package com.itv.chuckwagon.sbt

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.AliasName
import com.itv.aws.lambda.LambdaName
import com.itv.aws.lambda.VpcConfigDeclaration

case class Environment(name: String) {
  val aliasName = AliasName(name)
}
case class LambdaEnvironmentDeclaration(name: LambdaName,
                                        vpcConfigDeclaration: VpcConfigDeclaration,
                                        roleArn: Option[ARN])
