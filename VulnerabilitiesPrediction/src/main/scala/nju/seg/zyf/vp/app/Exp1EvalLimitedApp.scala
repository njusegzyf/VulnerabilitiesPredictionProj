package nju.seg.zyf.vp.app

import java.nio.file.{ Files, Path }
import java.util
import java.util.stream.Collectors
import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.ITrainData.{ AmountTrainData, CategoryTrainData, Feature, ImpactTrainData }
import nju.seg.zyf.vp.RnnUtils.EncoderStateData
import nju.seg.zyf.vp.{ RnnUtils, WekaModel, WekaUtils }
import weka.classifiers.AbstractClassifier

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp1EvalLimitedApp extends App {

  // this.evalExp1Category()
  this.evalExp1Amount()
  this.evalExp1Impact()

  private def evalExp1Category(): Unit = {

    val rnnLogFile: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1CategoryPrefix }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    // limit rnn data size
    val rnnData: EncoderStateData[Double] =
      RnnUtils.parseRnnLogForce(rnnLogLines, Exp1Configs.exp1RnnLayer, Exp1Configs.exp1RnnUnitNum)
      .limitSizeTo(Exp1Configs.exp1DataInstancesSizeLimitForLimitedAlgorithms)

    val categoryLogFile: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1CategoryPrefix }_Data_Category.txt")
    val categoryLogLinesAsJava: util.List[String] = Files.lines(categoryLogFile).limit(rnnData.size).collect(Collectors.toList())
    val categoryLogLines: Vector[String] = collectionAsScalaIterable(categoryLogLinesAsJava).toVector

    assert(rnnData.size == categoryLogLines.size)

    val trainData: CategoryTrainData = CategoryTrainData(rnnData, categoryLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp1Configs.exp1CategoryPrefix }_"
    WekaUtils.saveContentToArffFile(trainData, Exp1Configs.exp1PathConfig.trainDataDir, arffFileName)

    val evalAlgorithms: Vector[() => AbstractClassifier] = Exp1Configs.getExp1EvalLimitedAlgorithms(Feature.Category)
    for (evalAlgorithm <- evalAlgorithms) {
      val model = new WekaModel("Exp1", "", Feature.Category, evalAlgorithm(), Exp1Configs.exp1PathConfig)
      model.trainAndCrossValidate()
    }
  }

  private def evalExp1Amount(): Unit = {

    val rnnLogFile: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1AmountPrefix }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    // limit rnn data size
    val rnnData: EncoderStateData[Double] =
      RnnUtils.parseRnnLogForce(rnnLogLines, Exp1Configs.exp1RnnLayer, Exp1Configs.exp1RnnUnitNum)
      .limitSizeTo(Exp1Configs.exp1DataInstancesSizeLimitForLimitedAlgorithms)

    val amount: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1AmountPrefix }_Data_Amount.txt")
    val amountLogLinesAsJava: util.List[String] = Files.lines(amount).limit(rnnData.size).collect(Collectors.toList())
    val amountLogLines: Vector[Double] =
      collectionAsScalaIterable(amountLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == amountLogLines.size)

    val trainData: AmountTrainData = AmountTrainData(rnnData, amountLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp1Configs.exp1AmountPrefix }_"
    WekaUtils.saveContentToArffFile(trainData, Exp1Configs.exp1PathConfig.trainDataDir, arffFileName)

    val evalAlgorithms: Vector[() => AbstractClassifier] = Exp1Configs.getExp1EvalLimitedAlgorithms(Feature.Amount)
    for (evalAlgorithm <- evalAlgorithms) {
      val model = new WekaModel("Exp1", "", Feature.Amount, evalAlgorithm(), Exp1Configs.exp1PathConfig)
      model.trainAndCrossValidate()
    }
  }

  private def evalExp1Impact(): Unit = {

    val rnnLogFile: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1ImpactPrefix }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    // limit rnn data size
    val rnnData: EncoderStateData[Double] =
      RnnUtils.parseRnnLogForce(rnnLogLines, Exp1Configs.exp1RnnLayer, Exp1Configs.exp1RnnUnitNum)
      .limitSizeTo(Exp1Configs.exp1DataInstancesSizeLimitForLimitedAlgorithms)

    val impact: Path = Exp1Configs.exp1PathConfig.trainDataDir.resolve(raw"${ Exp1Configs.exp1ImpactPrefix }_Data_Impact.txt")
    val impactLogLinesAsJava: util.List[String] = Files.lines(impact).limit(rnnData.size).collect(Collectors.toList())
    val impactLogLines: Vector[Double] =
      collectionAsScalaIterable(impactLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == impactLogLines.size)

    val trainData: ImpactTrainData = ImpactTrainData(rnnData, impactLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp1Configs.exp1ImpactPrefix }_"
    WekaUtils.saveContentToArffFile(trainData, Exp1Configs.exp1PathConfig.trainDataDir, arffFileName)

    val evalAlgorithms: Vector[() => AbstractClassifier] = Exp1Configs.getExp1EvalLimitedAlgorithms(Feature.Impact)
    for (evalAlgorithm <- evalAlgorithms) {
      val model = new WekaModel("Exp1", "", Feature.Impact, evalAlgorithm(), Exp1Configs.exp1PathConfig)
      model.trainAndCrossValidate()
    }
  }
}
