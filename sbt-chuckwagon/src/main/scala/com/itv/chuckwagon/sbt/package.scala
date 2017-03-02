package com.itv.chuckwagon.sbt

import com.itv.aws.iam.ARN
import com.itv.aws.lambda.{AliasName, LambdaName, VpcConfigDeclaration}

case class Environment(name: String) {
  val aliasName = AliasName(name)
}
case class LambdaEnvironmentDeclaration(name: LambdaName,
                                        vpcConfigDeclaration: VpcConfigDeclaration,
                                        roleArn: Option[ARN])
