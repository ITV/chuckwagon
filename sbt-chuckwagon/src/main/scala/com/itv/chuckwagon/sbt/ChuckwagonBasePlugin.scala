package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.deploy.AWSCompiler
import fansi.Color.Green
import fansi.Str
import sbt._
import sbt.Keys._
import LoggingUtils._

object ChuckwagonBasePlugin extends AutoPlugin {

  object autoImport extends Keys.Base
  import autoImport._

  override lazy val projectSettings =
    Seq(
      chuckVpnConfigDeclaration := None,
      chuckSDKFreeCompiler := new AWSCompiler(
        com.itv.aws.lambda.awsLambda(chuckLambdaRegion.value)
      ),
      chuckVpcConfig := {
        val maybeVpcConfig =
          com.itv.chuckwagon.deploy
            .getVpcConfig(
              chuckVpnConfigDeclaration.value
            )
            .foldMap(chuckSDKFreeCompiler.value.compiler)

        maybeVpcConfig match {
          case Some(vpcConfig) => {
            streams.value.log.info(logMessage(
              (Str("Desired vpc-id: '") ++ Green(vpcConfig.vpc.id) ++ Str("' subnets: '") ++ vpcConfig.subnets.map(s => Green(s.id).render).mkString(Str(", ").render) ++ Str("' security groups: '") ++ vpcConfig.securityGroups.map(sg => Green(sg.id).render).mkString(Str(", ").render) ++ Str("'")).render
            ))
          }
          case None =>
            streams.value.log.info(logMessage(
              "No vpcConfigDeclaration defined so cannot lookup vpcConfig"
            ))
        }
        maybeVpcConfig
      }
    )
}