package com.itv.chuckwagon.deploy

import ammonite.ops._

class CommandRecorder(name: String) extends (DeployLambdaA[_] => Unit) {
  import CommandRecorder._
  private var commands: List[DeployLambdaA[_]] = Nil

  def result: List[DeployLambdaA[_]] = commands.reverse

  override def apply(next: DeployLambdaA[_]): Unit =
    commands = next :: commands

  def writeToFile(): Unit =
    write.over(CommandRecorder.wd / pprintFileName(name), pprint.tokenize(result).mkString)

  def readFromFile(): String =
    readCommandRecorderFile(name)
}

object CommandRecorder {
  val wd = pwd / "target" / "command-recorder"

  def pprintFileName(name: String): String = s"$name.pprinted"

  def readCommandRecorderFile(name: String): String =
    read(wd / pprintFileName(name))
}
