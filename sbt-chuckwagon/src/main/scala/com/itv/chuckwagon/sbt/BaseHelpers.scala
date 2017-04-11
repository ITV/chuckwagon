package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.VpcConfig
import com.itv.chuckwagon.deploy.VpcConfigLookup
import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport.chuckSDKFreeCompiler
import com.itv.chuckwagon.sbt.LoggingUtils.logMessage
import fansi.Color.Green
import fansi.Str
import sbt.Def
import sbt.Task
import sbt.Keys.streams

object BaseHelpers {

  def maybeVpcConfig(
      maybeVpcConfigDeclaration: Option[VpcConfigLookup]
  ): Def.Initialize[Task[Option[VpcConfig]]] = Def.taskDyn {
    maybeVpcConfigDeclaration match {
      case None                       => Def.task(None)
      case Some(vpcConfigDeclaration) => vpcConfig(vpcConfigDeclaration)
    }
  }

  def vpcConfig(vpcConfigDeclaration: VpcConfigLookup): Def.Initialize[Task[Option[VpcConfig]]] =
    Def.taskDyn {
      val maybeVpcConfig: Option[VpcConfig] =
        com.itv.chuckwagon.deploy.VpcCommands
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
          )
        )
      }
      Def.task(maybeVpcConfig)
    }

}
