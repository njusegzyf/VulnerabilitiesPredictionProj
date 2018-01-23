package nju.seg.zyf.vp

import scala.collection.mutable
import scala.compat.Platform

import java.io.IOException
import java.nio.file.Path
import java.util.Random
import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Charsets

import org.slf4j.{ Logger, LoggerFactory }

import nju.seg.zyf.vp.ITrainData.Feature
import nju.seg.zyf.vp.IWekaMidResult.{ ClassificationResultForInstance, RegressionResultForInstance, WekaClassificationMidResult, WekaRegressionMidResult }
import nju.seg.zyf.vp.Utils.PathConfig
import nju.seg.zyf.vp.WekaModel.{ WekaModelInfo, WekaModelType }
import weka.classifiers.{ Classifier, Evaluation }
import weka.core.{ Instances, SerializationHelper }
import weka.core.converters.ConverterUtils.DataSource

/**
  * @note This class refers to class [[weka.ModelTrain]].
  *
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
final class WekaModel(val modelName: String,
                      val remark: String,
                      val feature: ITrainData.Feature.Value,
                      implicit val classifier: Classifier,
                      val pathConfig: PathConfig,
                      val putRemarkAfterFeature: Boolean = false) {

  require(this.modelName != null) // require(!Strings.isNullOrEmpty(this.modelName))
  require(this.remark != null) // require(!Strings.isNullOrEmpty(this.remark))
  require(this.feature != null)
  require(this.classifier != null)
  require(this.pathConfig != null)

  @Nonnull
  val filePrefix: String = s"${ this.modelName }_${ this.feature.toString }"

  @Nonnull
  val classifierName: String = this.classifier.getClass.getSimpleName

  /** Trains the model (build and save the classifier).
    *
    * @note This method refers to methods `crossValidate2` in class [[weka.ModelTrain]].
    */
  @Nonnull @CheckReturnValue
  private def train(): Instances = {
    WekaModel.logger.info("Begin model train.")

    val trainDataFile: Path = this.pathConfig.trainDataDir.resolve(s"${ this.filePrefix }_${ this.remark }.arff")
    WekaModel.logger.info(s"Load train data from: ${ trainDataFile.toAbsolutePath.toString }.")
    val trainData: Instances = DataSource.read(trainDataFile.toAbsolutePath.toString)
    // take the lass attribute as class data
    val classIndex: Int = trainData.numAttributes() - 1
    trainData.setClassIndex(classIndex)

    // record the time to train the model/classifier
    val startMillis: Long = System.currentTimeMillis()

    val algorithmName: String = this.classifier.getClass.getSimpleName

    WekaModel.logger.info(s"Train classifier with algorithm: $algorithmName.")
    this.classifier.buildClassifier(trainData)

    val endMillis: Long = System.currentTimeMillis()
    WekaModel.logger.info(s"End train in ${ (endMillis - startMillis).toDouble / 1000.0 } seconds.")

    val classifierFile: Path = this.classifierFilePath

    WekaModel.logger.info(s"Save trained classifier to: ${ classifierFile.toAbsolutePath.toString }.")
    SerializationHelper.write(classifierFile.toAbsolutePath.toString, this.classifier)

    WekaModel.logger.info("End model train.")

    trainData
  }

  /** Evaluates the model using cross-validation.
    *
    * @note This method refers to methods `crossValidate2` in class [[weka.ModelTrain]].
    */
  def trainAndCrossValidate(@Nonnegative foldsNum: Int = 10,
                            rand: Random = new Random(0))
  : Unit = {
    require(foldsNum > 0)
    require(rand != null)

    val trainData = this.train()

    val modelType = WekaModel.featureToModelType(this.feature)
    WekaModel.logger.info(s"Begin model evaluation with $foldsNum folds, using model : ${ modelType.toString }.")

    val randData: Instances = new Instances(trainData)
    randData.randomize(rand)
    randData.stratify(foldsNum)
    val eval: Evaluation = new Evaluation(randData)

    val resultFileContent: StringBuilder = new StringBuilder
    def infoToLoggerAndResultFile(str: String): Unit = {
      WekaModel.logger.info(str)
      resultFileContent.append(str)
      resultFileContent.append(Platform.EOL)
    }

    if (modelType == WekaModelType.Classification) {

      val foldResultsBuilder: mutable.Builder[Vector[ClassificationResultForInstance], Vector[Vector[ClassificationResultForInstance]]] =
        Vector.newBuilder[Vector[ClassificationResultForInstance]]

      val fRanks = Range(0, foldsNum) map { i =>
        val train = randData.trainCV(foldsNum, i)
        val test = randData.testCV(foldsNum, i)

        // compute mid results for one fold
        import scala.collection.JavaConverters.asScalaIterator
        foldResultsBuilder += asScalaIterator(test.iterator()).map { WekaUtils.computeClassificationResult }
                              .toVector

        this.classifier.buildClassifier(train)
        eval.evaluateModel(this.classifier, test)
        val fRank = weka.WekaUtils.calFRank(this.classifier, test)
        infoToLoggerAndResultFile(s"FRank$i: " + fRank)
        fRank
      }

      val foldResults: Vector[Vector[ClassificationResultForInstance]] = foldResultsBuilder.result()
      // write mid result to file
      ObjectToFile.Create[WekaClassificationMidResult](this.midResultFilePath.toFile)
      .WriteObject(WekaClassificationMidResult(this.modelInfo, content = foldResults),
                   true)

      SerializationHelper.write(this.evalFilePath.toAbsolutePath.toString, eval)
      infoToLoggerAndResultFile(eval.toSummaryString())
      infoToLoggerAndResultFile("Average FRank: " + fRanks.sum / fRanks.size)

    } else {

      assert(modelType == WekaModelType.Regression)

      val foldResultsBuilder: mutable.Builder[Vector[RegressionResultForInstance], Vector[Vector[RegressionResultForInstance]]] =
        Vector.newBuilder[Vector[RegressionResultForInstance]]

      val creAndCre2s = Range(0, foldsNum) map { i =>
        val train: Instances = randData.trainCV(foldsNum, i)
        val test: Instances = randData.testCV(foldsNum, i)

        // compute mid results for one fold
        import scala.collection.JavaConverters.asScalaIterator
        foldResultsBuilder += asScalaIterator(test.iterator()).map { WekaUtils.computeRegressionResult }
                              .toVector

        this.classifier.buildClassifier(train)
        eval.evaluateModel(this.classifier, test)
        val cre = weka.WekaUtils.calCRE(this.classifier, test)
        val cre2 = weka.WekaUtils.calCRE2(this.classifier, test)
        infoToLoggerAndResultFile(s"CCRE$i: " + cre)
        infoToLoggerAndResultFile(s"CRE$i: " + cre2)
        (cre, cre2)
      }

      val foldResults: Vector[Vector[RegressionResultForInstance]] = foldResultsBuilder.result()
      // write mid result to file
      ObjectToFile.Create[WekaRegressionMidResult](this.midResultFilePath.toFile)
      .WriteObject(WekaRegressionMidResult(this.modelInfo, content = foldResults),
                   true)

      SerializationHelper.write(this.evalFilePath.toAbsolutePath.toString, eval)
      infoToLoggerAndResultFile(eval.toSummaryString())
      val ccres = creAndCre2s map { _._1 }
      val cres = creAndCre2s map { _._2 }
      infoToLoggerAndResultFile("Avg CCRE: " + ccres.sum / ccres.size)
      infoToLoggerAndResultFile("Avg CRE: " + cres.sum / cres.size)
    }

    // write result file
    try {
      import com.google.common.io.Files.asCharSink
      asCharSink(this.resultFilePath.toFile, Charsets.UTF_8).write(resultFileContent)
    } catch {
      case _: IOException =>
    }

    WekaModel.logger.info("End model evaluation.")
  }

  @Nonnull @CheckReturnValue
  private def classifierFilePath: Path = {
    if (this.putRemarkAfterFeature)
      this.pathConfig.modelDir resolve s"${ this.filePrefix }_${ this.remark }_${ this.classifierName }.cls"
    else
      this.pathConfig.modelDir resolve s"${ this.filePrefix }_${ this.classifierName }_${ this.remark }.cls"
  }

  @Nonnull @CheckReturnValue
  private def resultFilePath: Path =
    if (this.putRemarkAfterFeature)
      this.pathConfig.evaluationDir resolve s"${ this.filePrefix }_${ this.remark }_${ this.classifierName }_accuracy_measure.txt"
    else
      this.pathConfig.evaluationDir resolve s"${ this.filePrefix }_${ this.classifierName }_accuracy_measure_${ this.remark }.txt"

  @Nonnull @CheckReturnValue
  private def evalFilePath: Path =
    if (this.putRemarkAfterFeature)
      this.pathConfig.evaluationDir resolve s"${ this.filePrefix }_${ this.remark }_${ this.classifierName }_evaluation.eval"
    else
      this.pathConfig.evaluationDir resolve s"${ this.filePrefix }_${ this.classifierName }_evaluation_${ this.remark }.eval"

  @Nonnull @CheckReturnValue
  private def midResultFilePath: Path =
    if (this.putRemarkAfterFeature)
      this.pathConfig.wekaMiddleResultDir resolve s"${ this.filePrefix }_${ this.remark }_${ this.classifierName }_${ WekaModel.middleResultFilePostfix }"
    else
      this.pathConfig.wekaMiddleResultDir resolve s"${ this.filePrefix }_${ this.classifierName }_${ this.remark }_${ WekaModel.middleResultFilePostfix }"

  @Nonnull @CheckReturnValue
  def modelInfo: WekaModelInfo = WekaModelInfo(this.modelName, this.remark, this.feature.toString, this.classifierName)
}

object WekaModel {

  object WekaModelType extends Enumeration {
    val Classification: WekaModelType.Value = Value("Classification")
    val Regression: WekaModelType.Value = Value("Regression")
  }

  @Nonnull @CheckReturnValue
  def featureToModelType(feature: Feature.Value): WekaModelType.Value = {
    require(feature != null)

    feature match {
      case Feature.Category => WekaModelType.Classification
      case Feature.Amount   => WekaModelType.Regression
      case Feature.Impact   => WekaModelType.Regression
    }
  }

  @Nonnull
  val middleResultFilePostfix: String = "MidResult"

  @CheckReturnValue
  def isMiddleResultFile(filePath: Path): Boolean = {
    require(filePath != null)

    // Note: `endsWith` method in `Path` is different from `endsWith` method in `String
    filePath.getFileName.toString endsWith this.middleResultFilePostfix // && Files.isRegularFile(filePath)
  }

  @SerialVersionUID(0L)
  final case class WekaModelInfo(modelName: String,
                                 remark: String,
                                 featureName: String,
                                 classifierName: String)
    extends Serializable

  private val logger: Logger = LoggerFactory.getLogger(classOf[WekaModel])
}
