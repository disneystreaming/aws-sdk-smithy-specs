# Archived

This repository has been superseded by:

- AWS publishing official Smithy specs for their services: https://aws.amazon.com/blogs/aws/introducing-aws-api-models-and-publicly-available-resources-for-aws-api-definitions/ (repo: https://github.com/aws/api-models-aws)
- smithy4s using those: https://disneystreaming.github.io/smithy4s/docs/protocols/aws/aws

![Maven Central](https://img.shields.io/maven-central/v/com.disneystreaming.smithy/aws-kinesis-spec)

#### aws-sdk-specs

This repository aims at building and publishing artifacts containing the smithy specifications for the AWS SDK.

The specification in question are found there https://github.com/aws/aws-sdk-js-v3/tree/main/codegen.

The artifacts are published at the following Maven Central coordinates :

```scala
com.disneystreaming.smithy:aws-${SERVICE}-spec:version
```

For instance, for dynamodb, the coordinates are

```scala
com.disneystreaming.smithy:aws-dynamodb-spec:version
```
