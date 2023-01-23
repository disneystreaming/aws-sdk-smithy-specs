import mill.define.Sources
import $ivy.`software.amazon.smithy:smithy-model:1.27.1`
import $ivy.`software.amazon.smithy:smithy-rules-engine:1.27.1`
import $ivy.`software.amazon.smithy:smithy-build:1.27.1`
import $ivy.`software.amazon.smithy:smithy-aws-traits:1.27.1`
import $ivy.`software.amazon.smithy:smithy-waiters:1.27.1`

import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import software.amazon.smithy.model.Model
import java.time.format.DateTimeFormatter
import java.time.DateTimeException
import java.nio.file.Path
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import scala.jdk.CollectionConverters._

val smithyVersion = "1.27.1"
val smithyDeps = ivy"software.amazon.smithy::smithy-rules-engine:smithyVersion"

def specFolder =
  os.pwd / "aws-sdk-js-v3" / "codegen" / "sdk-codegen" / "aws-models"

val allSpecs = os
  .list(specFolder)
  .map(_.last)
  .filter(_.endsWith(".json"))
  .map(_.dropRight(".json".length()))

def writeAllSpecs = T {
  T.traverse(allSpecs.map(aws(_)))(_.writeForCheckIn())
}

object aws extends Cross[AWSSpec](allSpecs: _*)

class AWSSpec(service: String) extends JavaModule with PublishModule {

  val namespace = service.filter(_.isLetterOrDigit)
  val fullFileName = s"com.amazonaws.$namespace.smithy"
  val shortFileName = s"$namespace.smithy"
  override def artifactName: T[String] = T(s"aws-$service-spec")

  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"software.amazon.smithy:smithy-aws-traits:$smithyVersion",
    ivy"software.amazon.smithy:smithy-waiters:$smithyVersion"
  )

  def writeForCheckIn() = T.task {
    os.write(
      T.workspace / "specs" / shortFileName,
      trimmedModel(),
      createFolders = true
    )

  }

  def spec = T.source(specFolder / s"$service.json")

  def trimmedModel = T {
    val model = Model
      .assembler()
      .discoverModels(this.getClass().getClassLoader())
      .addImport(spec().path.toNIO)
      .assemble()
      .unwrap()
    val serializer: SmithyIdlModelSerializer = SmithyIdlModelSerializer
      .builder()
      .shapeFilter(_.getId().getNamespace() != "smithy.rules")
      .traitFilter(_.toShapeId().getNamespace() != "smithy.rules")
      .build()

    val map =
      serializer.serialize(model).asScala

    map(Path.of(fileName))
  }

  override def resources: Sources = T.sources {
    val target = T.dest / "META-INF" / "smithy" / shortFileName
    val manifestTarget = T.dest / "META-INF" / "smithy" / "manifest"
    os.write.over(target, trimmedModel(), createFolders = true)
    os.write.over(manifestTarget, shortFileName, createFolders = true)
    Seq(PathRef(target), PathRef(manifestTarget))
  }

  def publishVersion: T[String] = T.input {
    val today = java.time.LocalDate.now()
    "v" + today.format(DateTimeFormatter.ofPattern("YYYY.MM.DD"))
  }

  def pomSettings: T[PomSettings] = PomSettings(
    s"Jar containing smithy spec for $service",
    "com.disneystreaming.smithy",
    licenses = Seq(License.`Apache-2.0`),
    url = "http://github.com/disneystreaming/aws-sdk-specs",
    versionControl = VersionControl(
      Some("https://github.com/disneystreaming/aws-sdk-specs")
    ),
    developers = Seq(
      Developer(
        "baccata",
        "Olivier Mélois",
        "https://github.com/baccata"
      )
    )
  )
}
