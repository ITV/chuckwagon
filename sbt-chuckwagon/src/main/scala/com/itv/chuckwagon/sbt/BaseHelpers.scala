package com.itv.chuckwagon.sbt

import com.itv.aws.lambda.VpcConfig
import com.itv.chuckwagon.deploy.VpcConfigLookup
import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport.chuckSDKFreeCompiler
import sbt.Def
import sbt.Task

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
          .foldMap(chuckSDKFreeCompiler.value)

      Def.task(maybeVpcConfig)
    }

}
