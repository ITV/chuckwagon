---
layout: home
---
Chuckwagon is an [AWS Lambda][aws-lambda] Deployment Toolkit for Scala/sbt.

Its initial goals are to
* make sbt powered AWS Lambda [Continuous Delivery][steve-smiths-continuous-delivery] pipelines fun and easy to write. 
* complement existing AWS managed infrastructures in an organisation.

## Getting Started

It is beyond the scope of this section to explain how to write the Scala code for a Lambda, but here are some great blog posts on the subject if you need a primer,
 * [Writing Aws Lambdas In Scala - @yeghishe][yegishe-lambda-introduction]
 * [Scala and AWS Lambda Blueprints - @d6y][d6y-lambda-introduction]

Chuckwagon will always use your local default AWS credentials. For this primer those credentials will need to grant read and write access to EC2, IAM and Lambda. You will need to setup your `~/.aws/credentials` file as below, with the appropriate settings filled in for `<LABELLED_SECTIONS>`. 

```
[default]
region = <YOUR_REGION>
aws_access_key_id=<YOUR_ACCESS_KEY>
aws_secret_access_key=<YOUR_SECRET_KEY>
```

Chuckwagon is an sbt plugin. To install it add the following line to the `project/plugins.sbt` file in your Lambda project. 

```scala
addSbtPlugin("com.itv" % "sbt-chuckwagon" % "0.0.14")
```

Next add the following configuration to your Lambda project's `build.sbt` file with appropriate settings filled in for `<LABELLED_SECTIONS>`.

```scala
enablePlugins(com.itv.chuckwagon.sbt.ChuckwagonCreatePlugin)
chuckRegion := "<AN_AWS_REGION_EG_-_eu-west-1>"
chuckName := "<THE_NAME_YOU_WANT_FOR_YOUR_LAMBDA>"
chuckEnvironments := Set[String]("qa")
chuckCreateConfig := chuckCreateConfigBuilder
  .withHandler("<FULLY_QUALIFIED_LAMBDA_HANDLER_CLASS>::<HANDLER_METHOD>")
  .withMemorySizeInMB(192)
  .withTimeout("5 seconds")
  .withStagingBucketName("<THE_S3_BUCKET_WHER_CHUCKWAGON_WILL_UPLOAD_YOUR_CODE")
  .withCodeFile(assembly)
```

You are now ready to run the following Task and create your first AWS Lambda with Chuckwagon,

`sbt chuckCreate qa` - Create/Update a Lambda and push it to the QA environment

The first time you run this task it will do the following,
- Create a fat JAR out of your code and all of its dependencies using the [sbt-assembly] plugin.
- Upload your Assembly JAR to the S3 Bucket that you specified in the `chuckCreateConfig`.
- Create an IAM Role with the appropriate permissions for running the Lambda.
- Create a Lambda configured to use 
    - the runtime configuration that you provided in `chuckCreateConfig`
    - the Assembly JAR in the S3 Bucket
    - the IAM Role with appropriate permissions for running the Lambda
- Publish the Lambda (this creates a readonly copy of your Lambda and assigns stamps it with version number '**1**').
- Alias (the readonly) Version '**1**' of your Lambda with the name '**qa**' (as specified in the Task and as defined as an environment in `chuckCreateConfig`)

The second time you run `sbt chuckCreate qa` it will do the following,
- Create a new Assembly JAR and over-write the previous JAR in S3 with it.
- Check that the IAM Role still exists and still has the appropriate permissions. Recreate it or modify it if necessary.
- Update the existing Lambda to
    - have the runtime configuration that you provided in `chuckCreateConfig`
    - use the updated Assembly JAR in S3
    - have the appropriate IAM Role
- Publish the Lambda to create a new readonly copy with version number '**2**'.
- Update the '**qa**' alias to point at version '**2**'.
 
You can safely run `chuckCreate qa` as many times as you want. You can also always rely on it to make sure that the created or updated Lambda pointed to by the '**qa**' alias always matches the contents of `chuckCreateConfig`. It is equivalent to an idempotent REST `PUT` operation in this respect.

The only non-idempotent operation is the creation of a new version number each time.

If you want to keep track of how many versions of your Lambda you have published run,

`sbt chuckCurrentlyPublished` - The currently published versions of this Lambda (if Lambda exists)

You don't want to leave too many copies of your Lambda lying around so why not run,

`sbt chuckCleanUp` - Remove all Published Lambda Versions not attached to Aliases and all Aliases not defined in chuckEnvironments

----
Motivation
----

Starting to think we need a simpler tutorial...

AWS resources are always assigned uniquely addressable locations for targetting with specific API calls (eg invocation of your Lambda from the command line). Creating a Lambda with Chuckwagon gives us three ARNs for one Lambda, each of which has a very special meaning.

* `arn:aws:lambda:<REGION>:<ACCOUNT_ID>:function:<LAMBDA_NAME>` - will always be the latest version of your Lambda
* `arn:aws:lambda:<REGION>:<ACCOUNT_ID>:function:<LAMBDA_NAME>:1` - the very first version of your Lambda
* `arn:aws:lambda:<REGION>:<ACCOUNT_ID>:function:<LAMBDA_NAME>:qa`


[aws-lambda]: https://aws.amazon.com/lambda/ "AWS Lambda"
[steve-smiths-continuous-delivery]: http://www.alwaysagileconsulting.com/articles/what-is-continuous-delivery/ "Introduction to Continuous Delivery"
[yegishe-lambda-introduction]: http://yeghishe.github.io/2016/10/16/writing-aws-lambdas-in-scala.html "Writing Aws Lambdas In Scala"
[d6y-lambda-introduction]: http://underscore.io/blog/posts/2016/02/01/aws-lambda.html "Scala and AWS Lambda Blueprints"
[sbt-assembly]: https://github.com/sbt/sbt-assembly "sbt Assembly Plugin"