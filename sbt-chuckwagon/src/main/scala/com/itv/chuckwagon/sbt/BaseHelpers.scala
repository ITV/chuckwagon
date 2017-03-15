package com.itv.chuckwagon.sbt

import cats.Id
import com.itv.aws.lambda.{VpcConfig, VpcConfigDeclaration}
import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport.chuckSDKFreeCompiler
import com.itv.chuckwagon.sbt.LoggingUtils.logMessage
import fansi.Color.Green
import fansi.Str
import sbt.{Def, Task}
import sbt.Keys.streams

object BaseHelpers {

  def maybeVpcConfig(
      maybeVpcConfigDeclaration: Option[VpcConfigDeclaration]): Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    maybeVpcConfigDeclaration match {
      case None => {
        streams.value.log.info(
          logMessage(
            "No vpcConfigDeclaration defined so cannot lookup vpcConfig"
          ))
        Def.task(None)
      }
      case Some(vpcConfigDeclaration) => vpcConfig(vpcConfigDeclaration)
    }
  }

  def vpcConfig(vpcConfigDeclaration: VpcConfigDeclaration): Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    val maybeVpcConfig: Option[VpcConfig] =
      com.itv.chuckwagon.deploy
        .getVpcConfig(
          vpcConfigDeclaration
        )
        .foldMap(chuckSDKFreeCompiler.value.compiler)

    maybeVpcConfig.foreach { vpcConfig =>
      streams.value.log.info(
        logMessage(
          (Str("Desired vpc-id: '") ++ Green(vpcConfig.vpc.id) ++ Str("' subnets: '") ++ vpcConfig.subnets
            .map(s => Green(s.id).render)
            .mkString(Str(", ").render) ++ Str("' security groups: '") ++ vpcConfig.securityGroups
            .map(sg => Green(sg.id).render)
            .mkString(Str(", ").render) ++ Str("'")).render
        ))
    }
    Def.task(maybeVpcConfig)
  }

}
