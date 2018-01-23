package nju.seg.zyf.vp

import java.io.IOException
import java.nio.file.{ Files, Path }
import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Charsets

import nju.seg.zyf.vp.IWekaMidResult.IWeakMiddleResultForInstance
import nju.seg.zyf.vp.WekaModel.WekaModelInfo

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
@SerialVersionUID(0L)
sealed trait IWekaMidResult[TFoldData <: IWeakMiddleResultForInstance with Serializable] extends Serializable {

  @Nonnull @CheckReturnValue
  def modelInfo: WekaModelInfo

  @Nonnull @CheckReturnValue
  def content: Vector[Vector[TFoldData]]

  @Nonnull @CheckReturnValue
  final def apply(index: Int): Vector[TFoldData] = this.content(index)

  @Nonnull @CheckReturnValue
  final def foldNum: Int = this.content.size

  @Nonnull @CheckReturnValue
  def eval(): String = s"Middle result file of type ${ this.getClass.toString }."
}

object IWekaMidResult {

  sealed trait IWeakMiddleResultForInstance {

    @Nonnull @CheckReturnValue
    def toRecordLine: String
  }

  @SerialVersionUID(0L)
  final case class ClassificationResultForInstance(distributionForInstance: Array[Double], classValue: Int)
    extends IWeakMiddleResultForInstance
            with Serializable {

    @Nonnull @CheckReturnValue
    override def toRecordLine: String = {
      val lineBuilder = Vector.newBuilder[String]
      lineBuilder += classValue.toString
      lineBuilder ++= distributionForInstance map { _.toString }
      lineBuilder.result().mkString("\t")
    }
  }

  @SerialVersionUID(0L)
  final case class RegressionResultForInstance(predicateValue: Double, actualValue: Double)
    extends IWeakMiddleResultForInstance
            with Serializable {

    @Nonnull @CheckReturnValue
    override def toRecordLine: String = {
      Seq(actualValue, predicateValue).mkString("\t")
    }
  }

  @SerialVersionUID(0L)
  final case class WekaClassificationMidResult(override val modelInfo: WekaModelInfo,
                                               override val content: Vector[Vector[ClassificationResultForInstance]])
    extends IWekaMidResult[ClassificationResultForInstance]
            with Serializable

  @SerialVersionUID(0L)
  final case class WekaRegressionMidResult(override val modelInfo: WekaModelInfo,
                                           override val content: Vector[Vector[RegressionResultForInstance]])
    extends IWekaMidResult[RegressionResultForInstance]
            with Serializable

  @throws[IOException]
  def processMiddleResultFile[T](filePath: Path,
                                 classificationHandler: WekaClassificationMidResult => T,
                                 regressionHandler: WekaRegressionMidResult => T)
  : T = {
    require(filePath != null && Files.isReadable(filePath))
    require(classificationHandler != null)
    require(regressionHandler != null)

    val middleResult: IWekaMidResult[_] = ObjectToFile.Create(filePath.toFile).ReadObject()
    middleResult match {
      case classificationMidRes: WekaClassificationMidResult => classificationHandler(classificationMidRes)
      case regressionMidRes: WekaRegressionMidResult         => regressionHandler(regressionMidRes)
    }
  }

  @throws[IOException]
  def saveAsRecordFile(midRes: IWekaMidResult[_ <: IWeakMiddleResultForInstance],
                       saveRecordFile: Path)
  : Path = {
    require(midRes != null)
    val saveRecordFolder = saveRecordFile.getParent
    Files.createDirectories(saveRecordFolder)
    require(Files.isDirectory(saveRecordFolder))

    val lines: Vector[String] = midRes.content.flatten // join instance in all folds
                                .map { _.toRecordLine }

    // write to result file
    import scala.collection.convert.ImplicitConversionsToJava.`iterable asJava`
    import com.google.common.io.Files.asCharSink
    asCharSink(saveRecordFile.toFile, Charsets.UTF_8) writeLines lines
    saveRecordFile
  }

  def convertMiddleResultFilesToRecords(middleResultFilesDir: Path,
                                        saveRecordsDir: Path)
  : Array[Path] = {
    require(middleResultFilesDir != null && Files.isDirectory(middleResultFilesDir))
    require(saveRecordsDir != null)
    Files.createDirectories(saveRecordsDir)
    require(Files.isDirectory(saveRecordsDir))

    Files.list(middleResultFilesDir)
    .filter { WekaModel.isMiddleResultFile }
    .map { (midResFilePath: Path) =>
      val middleResult: IWekaMidResult[_ <: IWeakMiddleResultForInstance] = ObjectToFile.Create(midResFilePath.toFile).ReadObject()
      println(raw"Save middle result file : ${ midResFilePath.getFileName } as record. ")
      val saveRecordFileName = s"${ midResFilePath.getFileName.toString }_Record"
      val saveRecordFile = saveRecordsDir resolve saveRecordFileName
      IWekaMidResult.saveAsRecordFile(middleResult, saveRecordFile)
    }.toArray[Path] { new Array[Path](_) }
  }
}
