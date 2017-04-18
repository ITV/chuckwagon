[![Join the chat at https://gitter.im/itv/chuckwagon](https://badges.gitter.im/itv/chuckwagon.svg)](https://gitter.im/itv/chuckwagon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Chuckwagon

Chuckwagon is a Scala/sbt [AWS Lambda](https://aws.amazon.com/lambda/) Toolkit. It makes creating and maintaining [Continuous Delivery](http://www.alwaysagileconsulting.com/articles/what-is-continuous-delivery/) pipelines typesafe and declarative.

Complete introductory guides and reference documentation available on the website,

[http://io.itv.com/chuckwagon/](http://io.itv.com/chuckwagon/)

# Installation

Add the following to your `project/plugins.sbt` file:


```scala
addSbtPlugin("com.itv.chuckwagon" % "sbt-chuckwagon" % "0.1.0")
```

Here is an example of the most basic possible configuration in your `build.sbt`:

```scala
enablePlugins(ChuckwagonPublishPlugin)
chuckRegion := "<AN_AWS_REGION_EG_-_eu-west-1>"
chuckPublishConfig := chuckPublishConfigBuilder
  .withName("<THE_NAME_YOU_WANT_FOR_YOUR_LAMBDA>")  
  .withHandler("<FULLY_QUALIFIED_CLASS::METHOD>")
  .withMemorySizeInMB(192)
  .withTimeout("5 seconds")
  .withStagingBucketName("<THE_S3_BUCKET_WHERE_CHUCKWAGON_WILL_UPLOAD_YOUR_CODE")
  .withCodeFile(assembly)
```

# Usage

```chuckPublishSnapshot``` - Will create/update your AWS Lambda

# Features 

* [Versioning / Environments](http://io.itv.com/chuckwagon/#DeploymentPipelines) (also known as [Aliases](http://docs.aws.amazon.com/lambda/latest/dg/versioning-aliases.html))
* [Deployment Pipelines using sbt-release](http://io.itv.com/chuckwagon/#sbt-release) 
* [Environment Specific Configurations](http://io.itv.com/chuckwagon/#EnvironmentConfiguration)
* [Copying between AWS Accounts](http://io.itv.com/chuckwagon/#MultipleAWSAccounts)
* [VPCs](http://io.itv.com/chuckwagon/#VPCs)
* [Many more](http://io.itv.com/chuckwagon/#Reference) including,
  * Scheduled Executions
  * Multiple Lambdas created from the same Assembly/Config
  * Invocation / Introspection of AWS from sbt shell

# Documentation

Complete documentation is available on the Chuckwagon website:

[http://io.itv.com/chuckwagon/](http://io.itv.com/chuckwagon/)

# Licence

Chuckwagon is free to use under the [ITV OSS Licence](http://io.itv.com/chuckwagon/) (a derivative of the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)).