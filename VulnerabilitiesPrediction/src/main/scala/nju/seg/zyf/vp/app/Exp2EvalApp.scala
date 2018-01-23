package nju.seg.zyf.vp.app

import java.nio.file.{ Files, Path }
import java.util
import java.util.stream.Collectors
import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.ITrainData.{ AmountTrainData, CategoryTrainData, Feature, ImpactTrainData }
import nju.seg.zyf.vp.RnnUtils.EncoderStateData
import nju.seg.zyf.vp.{ RnnUtils, WekaModel, WekaUtils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp2EvalApp extends App {

  // evaluate code 1
  for {evalMethod <- Seq[String => Unit](this.evalExp2CategoryForOneKeyCombination,
                                         this.evalExp2AmountForOneKeyCombination,
                                         this.evalExp2ImpactForOneKeyCombination)
       keyCombination <- Exp2Configs.exp2AllKeyCombinations
  } {
    evalMethod(keyCombination)
  }

  //region Evaluate code 2 (same effect as eval code 1)

  //  for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
  //    Exp2EvalApp.evalExp2CategoryForOneKeyCombination(keyCombination)
  //  }
  //
  //  for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
  //    Exp2EvalApp.evalExp2AmountForOneKeyCombination(keyCombination)
  //  }
  //
  //  for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
  //    Exp2EvalApp.evalExp2ImpactForOneKeyCombination(keyCombination)
  //  }

  //endregion Evaluate code 2 (same effect as eval code 1)

  private def evalExp2CategoryForOneKeyCombination(keyCombination: String): Unit = {

    val rnnLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2CategoryPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp2Configs.exp2RnnLayer, Exp2Configs.exp2RnnUnitNum)

    val categoryLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2CategoryPrefix }_${ keyCombination }_Data_Category.txt")
    val categoryLogLinesAsJava: util.List[String] = Files.lines(categoryLogFile).limit(rnnData.size).collect(Collectors.toList())
    val categoryLogLines: Vector[String] = collectionAsScalaIterable(categoryLogLinesAsJava).toVector

    assert(rnnData.size == categoryLogLines.size)

    val trainData: CategoryTrainData = CategoryTrainData(rnnData, categoryLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp2Configs.exp2CategoryPrefix }_${ keyCombination }"
    WekaUtils.saveContentToArffFile(trainData, Exp2Configs.exp2PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel("Exp2", keyCombination, Feature.Category, Exp2Configs.exp2Algorithm(), Exp2Configs.exp2PathConfig, true)
    model.trainAndCrossValidate()
  }

  private def evalExp2AmountForOneKeyCombination(keyCombination: String): Unit = {

    val rnnLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2AmountPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp2Configs.exp2RnnLayer, Exp2Configs.exp2RnnUnitNum)

    val amountLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2AmountPrefix }_${ keyCombination }_Data_Amount.txt")
    val amountLogLinesAsJava: util.List[String] = Files.lines(amountLogFile).limit(rnnData.size).collect(Collectors.toList())
    val amountLogLines: Vector[Double] =
      collectionAsScalaIterable(amountLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == amountLogLines.size)

    val trainData: AmountTrainData = AmountTrainData(rnnData, amountLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp2Configs.exp2AmountPrefix }_${ keyCombination }"
    WekaUtils.saveContentToArffFile(trainData, Exp2Configs.exp2PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel("Exp2", keyCombination, Feature.Amount, Exp2Configs.exp2Algorithm(), Exp2Configs.exp2PathConfig, true)
    model.trainAndCrossValidate()
  }

  private def evalExp2ImpactForOneKeyCombination(keyCombination: String): Unit = {

    val rnnLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2ImpactPrefix }_${ keyCombination }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp2Configs.exp2RnnLayer, Exp2Configs.exp2RnnUnitNum)

    val impactLogFile: Path =
      Exp2Configs.exp2PathConfig.trainDataDir.resolve(raw"${ Exp2Configs.exp2ImpactPrefix }_${ keyCombination }_Data_Impact.txt")
    val impactLogLinesAsJava: util.List[String] = Files.lines(impactLogFile).limit(rnnData.size).collect(Collectors.toList())
    val impactLogLines: Vector[Double] =
      collectionAsScalaIterable(impactLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == impactLogLines.size)

    val trainData: ImpactTrainData = ImpactTrainData(rnnData, impactLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp2Configs.exp2ImpactPrefix }_${ keyCombination }"
    WekaUtils.saveContentToArffFile(trainData, Exp2Configs.exp2PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel("Exp2", keyCombination, Feature.Impact, Exp2Configs.exp2Algorithm(), Exp2Configs.exp2PathConfig, true)
    model.trainAndCrossValidate()
  }
}
