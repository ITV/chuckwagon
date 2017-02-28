package com.itv.chuckwagon.sbt

import com.itv.chuckwagon.sbt.ChuckwagonBasePlugin.autoImport._
import sbt._
import sbt.Keys._

object ChuckwagonCopyPlugin extends AutoPlugin {

  override def requires = com.itv.chuckwagon.sbt.ChuckwagonBasePlugin

  object autoImport extends Keys.Copy

  override lazy val projectSettings =
    Seq(
      chuckEnvironments := chuckDefineEnvironments("blue-prd", "prd")
    )
}
