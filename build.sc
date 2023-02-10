import software.amazon.smithy.model.traits.ProtocolDefinitionTrait
import $ivy.`io.chris-kipp::mill-ci-release::0.1.5`
import $ivy.`software.amazon.smithy:smithy-model:1.27.1`
import $ivy.`software.amazon.smithy:smithy-rules-engine:1.27.1`
import $ivy.`software.amazon.smithy:smithy-build:1.27.1`
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait
import $ivy.`software.amazon.smithy:smithy-aws-traits:1.27.1`
import $ivy.`software.amazon.smithy:smithy-aws-iam-traits:1.27.1`
import $ivy.`software.amazon.smithy:smithy-waiters:1.27.1`
import $ivy.`software.amazon.smithy:smithy-aws-cloudformation-traits:1.27.1`

import mill.define.Sources
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
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

val smithyVersion = "1.27.1"
val org = "com.disneystreaming.smithy"

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

object summary extends BaseModule {

  def pomDescription: String =
    "Jar containing a summary of published specs for AWS services"

  override def artifactName: T[String] = T(s"aws-spec-summary")

  def summaryJson = T {
    val artifacts = T.traverse(allSpecs.map(aws(_))) { specModule =>
      T.task {
        ujson.Obj(
          "service" -> ujson.Str(specModule.service),
          "organization" -> ujson.Str(specModule.pomSettings().organization),
          "name" -> ujson.Str(specModule.artifactName()),
          "protocol" -> specModule
            .protocol()
            .map(ujson.Str(_))
            .getOrElse(ujson.Null)
        )
      }
    }()
    ujson.Obj(
      "version" -> ujson.Str(publishVersion()),
      "artifacts" -> artifacts
    )
  }

  def resources: Sources = T.sources {
    val target = T.dest / "summary.json"
    os.write.over(target, summaryJson(), createFolders = true)
    target
  }

}

object aws extends Cross[AWSSpec](allSpecs: _*)

class AWSSpec(val service: String) extends BaseModule {

  val pomDescription: String = s"Jar containing smithy spec for $service"

  val namespace = service.filter(_.isLetterOrDigit)
  val fullFileName = s"com.amazonaws.$namespace.smithy"
  val shortFileName = s"$namespace.smithy"
  override def artifactName: T[String] = T(s"aws-$service-spec")

  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"software.amazon.smithy:smithy-aws-traits:$smithyVersion",
    ivy"software.amazon.smithy:smithy-aws-cloudformation-traits:$smithyVersion",
    ivy"software.amazon.smithy:smithy-aws-iam-traits:$smithyVersion",
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

  def assembleModel = T.task {
    Model
      .assembler()
      .discoverModels(this.getClass().getClassLoader())
      .addImport(spec().path.toNIO)
      .assemble()
      .unwrap()
  }

  def protocol: T[Option[String]] = T {
    val model = assembleModel()
    val protocols = model
      .getShapesWithTrait(classOf[ProtocolDefinitionTrait])
      .asScala
      .filter(_.getId().getNamespace().startsWith("aws"))
      .toList
    val appliedProtocol = protocols.toList.find { protocolShape =>
      !model.getShapesWithTrait(protocolShape).isEmpty
    }
    appliedProtocol.map(_.toShapeId.getName())
  }

  def trimmedModel = T {
    val model = assembleModel()
    val serializer: SmithyIdlModelSerializer = SmithyIdlModelSerializer
      .builder()
      .shapeFilter(_.getId().getNamespace() != "smithy.rules")
      .traitFilter(_.toShapeId().getNamespace() != "smithy.rules")
      .build()

    val map =
      serializer.serialize(model).asScala

    map(Path.of(fullFileName))
  }

  override def resources: Sources = T.sources {
    val target = T.dest / "META-INF" / "smithy" / shortFileName
    val manifestTarget = T.dest / "META-INF" / "smithy" / "manifest"
    os.write.over(target, trimmedModel(), createFolders = true)
    os.write.over(manifestTarget, shortFileName, createFolders = true)
    Seq(PathRef(T.dest))
  }

}

trait BaseModule extends JavaModule with CiReleaseModule {

  def pomDescription: String

  override def sonatypeHost = Some(SonatypeHost.s01)

  def pomSettings: T[PomSettings] = PomSettings(
    pomDescription,
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
