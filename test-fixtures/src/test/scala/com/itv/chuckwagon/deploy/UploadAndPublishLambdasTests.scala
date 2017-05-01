package com.itv.chuckwagon.deploy

import java.io.File

import com.itv.aws.iam.ARN
import com.itv.aws.lambda._
import com.itv.aws.s3._
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import scala.concurrent.duration._

class UploadAndPublishLambdasTests extends FlatSpec with Matchers {

  val testBucketName  = BucketName("test")
  val testS3KeyPrefix = S3KeyPrefix("test")
  val testFile        = new File("")
  val commonPublishCommands: List[DeployLambdaA[_]] = {
    List[DeployLambdaA[_]](
      ListBuckets(),
      CreateBucket(BucketName("test")),
      PutObject(Bucket(testBucketName), PutFile(testS3KeyPrefix, testFile))
    )
  }

  def fakeLambda(string: String): Lambda =
    Lambda(
      deployment = LambdaDeploymentConfiguration(
        name = LambdaName(string),
        roleARN = ARN(string),
        vpcConfig = None
      ),
      runtime = LambdaRuntimeConfiguration(
        handler = LambdaHandler(string),
        timeout = 30.seconds,
        memorySize = MemorySize(128),
        deadLetterARN = None
      )
    )

  def fakeLambdas(strings: String*): List[Lambda] =
    strings.map(fakeLambda).toList

  def fakeUploadAndPublishLambdas(lambdas: List[Lambda],
                                  fakeAWSCompiler: FakeAWSCompiler): List[PublishedLambda] =
    com.itv.chuckwagon.deploy
      .uploadAndPublishLambdas(
        lambdas,
        testBucketName,
        PutFile(
          testS3KeyPrefix,
          testFile
        )
      )
      .foldMap(fakeAWSCompiler.compiler)

  behavior.of("Fake AWS Compiler for uploadAndPublishLambdas")

  it should "command publishing a single Lambda that doesn't exist and return its reference" in {
    val commandRecorder = new CommandRecorder("first-single")
    val fakeAWSCompiler = FakeAWSCompiler(
      arnSnapshot = ARN("lambdaARN"),
      nextVersion = LambdaVersion(1),
      commandRecorder = commandRecorder
    )
    val lambdas = fakeLambdas("test")

    val publishedLambdas = fakeUploadAndPublishLambdas(lambdas, fakeAWSCompiler)

    publishedLambdas should equal(List(PublishedLambda(lambdas(0), LambdaVersion(1), ARN("lambdaARN:1"))))

    val expectedCommands = commonPublishCommands ++ List(
      ListPublishedLambdasWithName(LambdaName("test")),
      CreatePublishedLambda(lambdas(0), S3Location(Bucket(testBucketName), S3Key("s3Location")))
    )

    commandRecorder.result should equal(expectedCommands)

    commandRecorder.writeToFile()
    commandRecorder.readFromFile() should equal(pprint.stringify(expectedCommands))
  }

  it should "command publishing two Lambdas that don't exist and return their references" in {
    val commandRecorder = new CommandRecorder("first-multiple")
    val fakeAWSCompiler = FakeAWSCompiler(
      arnSnapshot = ARN("lambdaARN"),
      nextVersion = LambdaVersion(1),
      commandRecorder = commandRecorder
    )
    val lambdas = fakeLambdas("test1", "test2")

    val publishedLambdas = fakeUploadAndPublishLambdas(lambdas, fakeAWSCompiler)

    publishedLambdas should equal(
      List(
        PublishedLambda(lambdas(0), LambdaVersion(1), ARN("lambdaARN:1")),
        PublishedLambda(lambdas(1), LambdaVersion(1), ARN("lambdaARN:1"))
      )
    )

    val expectedCommands =
      commonPublishCommands ++ List(
        ListPublishedLambdasWithName(LambdaName("test1")),
        CreatePublishedLambda(lambdas(0), S3Location(Bucket(testBucketName), S3Key("s3Location"))),
        ListPublishedLambdasWithName(LambdaName("test2")),
        CreatePublishedLambda(lambdas(1), S3Location(Bucket(testBucketName), S3Key("s3Location")))
      )

    commandRecorder.result should equal(expectedCommands)

    commandRecorder.writeToFile()
    commandRecorder.readFromFile() should equal(pprint.stringify(expectedCommands))
  }

  it should "command updating a single Lambda that exists and return its reference" in {
    val commandRecorder = new CommandRecorder("second-single")
    val lambda          = fakeLambda("test")
    val fakeAWSCompiler = FakeAWSCompiler(
      arnSnapshot = ARN("lambdaARN"),
      nextVersion = LambdaVersion(2),
      commandRecorder = commandRecorder,
      lambdaForName = (name: LambdaName) => Option(lambda)
    )
    val lambdas = List(lambda)

    val publishedLambdas = fakeUploadAndPublishLambdas(lambdas, fakeAWSCompiler)

    publishedLambdas should equal(List(PublishedLambda(lambda, LambdaVersion(2), ARN("lambdaARN:2"))))

    val expectedCommands = commonPublishCommands ++ List(
      ListPublishedLambdasWithName(LambdaName("test")),
      UpdateLambdaConfiguration(lambda),
      UpdateCodeAndPublishLambda(lambda, S3Location(Bucket(testBucketName), S3Key("s3Location")))
    )

    commandRecorder.result should equal(expectedCommands)

    commandRecorder.writeToFile()
    commandRecorder.readFromFile() should equal(pprint.stringify(expectedCommands))
  }

  def assertListsEqual[A](actual: List[A], expected: List[A]): Unit = {
    withClue(s"actual: ${pprint.stringify(actual)}, expected: ${pprint.stringify(expected)}") {
      actual.size should equal(expected.size)
    }
    actual.zip(expected).zipWithIndex.foreach { result =>
      withClue(s"At index '${result._2}'") {
        result._1._1 should equal(result._1._2)
      }
    }
  }

  it should "command updating two Lambdas that exist and return their references" in {
    val commandRecorder = new CommandRecorder("second-multiple")
    val lambdas         = fakeLambdas("test1", "test2")
    val fakeAWSCompiler = FakeAWSCompiler(
      arnSnapshot = ARN("lambdaARN"),
      nextVersion = LambdaVersion(2),
      commandRecorder = commandRecorder,
      lambdaForName = (name: LambdaName) => lambdas.find(_.deployment.name == name)
    )

    val publishedLambdas = fakeUploadAndPublishLambdas(lambdas, fakeAWSCompiler)

    publishedLambdas should equal(
      List(
        PublishedLambda(lambdas(0), LambdaVersion(2), ARN("lambdaARN:2")),
        PublishedLambda(lambdas(1), LambdaVersion(2), ARN("lambdaARN:2"))
      )
    )

    val expectedCommands =
      commonPublishCommands ++ List(
        ListPublishedLambdasWithName(LambdaName("test1")),
        UpdateLambdaConfiguration(lambdas(0)),
        UpdateCodeAndPublishLambda(lambdas(0), S3Location(Bucket(testBucketName), S3Key("s3Location"))),
        ListPublishedLambdasWithName(LambdaName("test2")),
        UpdateLambdaConfiguration(lambdas(1)),
        UpdateCodeAndPublishLambda(lambdas(1), S3Location(Bucket(testBucketName), S3Key("s3Location")))
      )

    assertListsEqual(commandRecorder.result, expectedCommands)

    commandRecorder.writeToFile()
    commandRecorder.readFromFile() should equal(pprint.stringify(expectedCommands))
  }

}
