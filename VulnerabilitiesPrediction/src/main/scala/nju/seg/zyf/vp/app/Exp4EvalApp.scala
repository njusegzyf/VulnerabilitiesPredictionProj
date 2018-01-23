package nju.seg.zyf.vp.app

import java.nio.file.{ Files, Path }
import java.util
import java.util.stream.Collectors
import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.ITrainData.{ AmountTrainData, CategoryTrainData, Feature, ImpactTrainData }
import nju.seg.zyf.vp.RnnUtils.EncoderStateData
import nju.seg.zyf.vp.Utils.PathConfig
import nju.seg.zyf.vp.{ RnnUtils, WekaModel, WekaUtils }
import weka.classifiers.AbstractClassifier

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp4EvalApp extends App {

  import Exp4Configs.exp4PathConfig // imported as an implicit val

  this.evalExp4Category()
  this.evalExp4Amount()
  this.evalExp4Impact()

  private def evalExp4Category()(implicit pathConfig: PathConfig): Unit = {

    for (keyCombination <- Exp4Configs.exp4AllKeyCombinations) {
      val rnnLogFile: Path =
        pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4CategoryPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
      import scala.collection.JavaConverters.collectionAsScalaIterable
      val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
      val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
      val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp4Configs.exp4RnnLayer, Exp4Configs.exp4RnnUnitNum)

      val categoryLogFile: Path =
        pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4CategoryPrefix }_${ keyCombination }_Data_Category.txt")
      val categoryLogLinesAsJava: util.List[String] = Files.lines(categoryLogFile).limit(rnnData.size).collect(Collectors.toList())
      val categoryLogLines: Vector[String] = collectionAsScalaIterable(categoryLogLinesAsJava).toVector

      assert(rnnData.size == categoryLogLines.size)

      val trainData: CategoryTrainData = CategoryTrainData(rnnData, categoryLogLines)

      // save arff file to train dir
      val arffFileName: String = s"${ Exp4Configs.exp4CategoryPrefix }_${ keyCombination }"
      WekaUtils.saveContentToArffFile(trainData, pathConfig.trainDataDir, arffFileName)

      val evalAlgorithms: Vector[() => AbstractClassifier] = Exp4Configs.getExp4EvalAlgorithms(trainData.featureType)
      for (evalAlgorithm <- evalAlgorithms) {
        val model = new WekaModel(Exp4Configs.exp4Prefix, keyCombination, trainData.featureType, evalAlgorithm(), pathConfig, true)
        model.trainAndCrossValidate()
      }
    }
  }

  private def evalExp4Amount()(implicit pathConfig: PathConfig): Unit = {

    for (keyCombination <- Exp4Configs.exp4AllKeyCombinations) {
      val rnnLogFile: Path =
        pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4AmountPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
      import scala.collection.JavaConverters.collectionAsScalaIterable
      val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
      val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
      val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp4Configs.exp4RnnLayer, Exp4Configs.exp4RnnUnitNum)

      val amountLogFile: Path = pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4AmountPrefix }_${ keyCombination }_Data_Amount.txt")
      val amountLogLinesAsJava: util.List[String] = Files.lines(amountLogFile).limit(rnnData.size).collect(Collectors.toList())
      val amountLogLines: Vector[Double] =
        collectionAsScalaIterable(amountLogLinesAsJava)
        .map { _.toDouble }
        .toVector

      assert(rnnData.size == amountLogLines.size)

      val trainData: AmountTrainData = AmountTrainData(rnnData, amountLogLines)

      // save arff file to train dir
      val arffFileName: String = s"${ Exp4Configs.exp4AmountPrefix }_${ keyCombination }"
      WekaUtils.saveContentToArffFile(trainData, pathConfig.trainDataDir, arffFileName)

      val evalAlgorithms: Vector[() => AbstractClassifier] = Exp4Configs.getExp4EvalAlgorithms(trainData.featureType)
      for (evalAlgorithm <- evalAlgorithms) {
        val model = new WekaModel(Exp4Configs.exp4Prefix, keyCombination, trainData.featureType, evalAlgorithm(), pathConfig, true)
        model.trainAndCrossValidate()
      }
    }
  }

  private def evalExp4Impact()(implicit pathConfig: PathConfig): Unit = {

    for (keyCombination <- Exp4Configs.exp4AllKeyCombinations) {
      val rnnLogFile: Path =
        pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4ImpactPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
      import scala.collection.JavaConverters.collectionAsScalaIterable
      val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
      val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
      val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp4Configs.exp4RnnLayer, Exp4Configs.exp4RnnUnitNum)

      val impactLogFile: Path = pathConfig.trainDataDir.resolve(raw"${ Exp4Configs.exp4ImpactPrefix }_${ keyCombination }_Data_Impact.txt")
      val impactLogLinesAsJava: util.List[String] = Files.lines(impactLogFile).limit(rnnData.size).collect(Collectors.toList())
      val impactLogLines: Vector[Double] =
        collectionAsScalaIterable(impactLogLinesAsJava)
        .map { _.toDouble }
        .toVector

      assert(rnnData.size == impactLogLines.size)

      val trainData: ImpactTrainData = ImpactTrainData(rnnData, impactLogLines)

      // save arff file to train dir
      val arffFileName: String = s"${ Exp4Configs.exp4ImpactPrefix }_${ keyCombination }"
      WekaUtils.saveContentToArffFile(trainData, pathConfig.trainDataDir, arffFileName)

      val evalAlgorithms: Vector[() => AbstractClassifier] = Exp4Configs.getExp4EvalAlgorithms(trainData.featureType)
      for (evalAlgorithm <- evalAlgorithms) {
        val model = new WekaModel(Exp4Configs.exp4Prefix, keyCombination, trainData.featureType, evalAlgorithm(), pathConfig, true)
        model.trainAndCrossValidate()
      }
    }
  }
}
