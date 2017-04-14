# Chuckwagon

Chuckwagon is a Scala/sbt [AWS Lambda](https://aws.amazon.com/lambda/) Toolkit. It makes creating and maintaining [Continuous Delivery](http://www.alwaysagileconsulting.com/articles/what-is-continuous-delivery/) pipelines typesafe and declarative.

Complete introductory guides and reference documentation available on the website,

[http://io.itv.com/chuckwagon/](http://io.itv.com/chuckwagon/)

# Installation

Add the following to your `project/plugins.sbt` file:


```scala
addSbtPlugin("com.itv" % "sbt-chuckwagon" % "0.1.0")
```

Here is an example of the most basic possible configuration in your `build.sbt`:

```scala
enablePlugins(ChuckwagonPublishPlugin)
chuckRegion := "<AN_AWS_REGION_EG_-_eu-west-1>"
chuckName := "<THE_NAME_YOU_WANT_FOR_YOUR_LAMBDA>"
chuckPublishConfig := chuckPublishConfigBuilder
  .withHandler("<FULLY_QUALIFIED_CLASS::METHOD>")
  .withMemorySizeInMB(192)
  .withTimeout("5 seconds")
  .withStagingBucketName("<THE_S3_BUCKET_WHER_CHUCKWAGON_WILL_UPLOAD_YOUR_CODE")
  .withCodeFile(assembly)
```

# Usage

```chuckPublishSnapshot``` - Will create/update your AWS Lambda

# Documentation

This README represents a tiny fraction of the features documented on the Chuckwagon website:

[http://io.itv.com/chuckwagon/](http://io.itv.com/chuckwagon/)

# Licence

Chuckwagon is free to use under the [ITV OSS Licence](http://io.itv.com/chuckwagon/) (a derivative of the [Apache License, Version 2.0]).