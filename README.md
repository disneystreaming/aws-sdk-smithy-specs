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
