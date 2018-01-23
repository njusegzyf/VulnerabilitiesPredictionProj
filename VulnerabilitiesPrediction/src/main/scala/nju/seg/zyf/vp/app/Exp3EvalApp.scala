package nju.seg.zyf.vp.app

import java.nio.file.{ Files, Path }
import java.util
import java.util.stream.Collectors
import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import nju.seg.zyf.vp.ITrainData.{ AmountTrainData, CategoryTrainData, Feature, ImpactTrainData }
import nju.seg.zyf.vp.RnnUtils.EncoderStateData
import nju.seg.zyf.vp.{ RnnUtils, WekaModel, WekaUtils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp3EvalApp extends App {

  // Eval code 1
  for {evalMethod <- Seq[SearchEngine.Value => Unit](this.evalExp3CategoryForOneSearchEngine,
                                                     this.evalExp3AmountForOneSearchEngine,
                                                     this.evalExp3ImpactForOneSearchEngine)
       searchEngine <- Exp3Configs.exp3SearchEngines
  } {
    evalMethod(searchEngine)
  }

  //region Evaluate code 2 (same effect as evaluate code 1)

  //  Exp3Configs.exp3SearchEngines foreach this.evalExp3CategoryForOneSearchEngine
  //  Exp3Configs.exp3SearchEngines foreach this.evalExp3AmountForOneSearchEngine
  //  Exp3Configs.exp3SearchEngines foreach this.evalExp3ImpactForOneSearchEngine

  //endregion Evaluate code 2 (same effect as evaluate code 1)

  //region Evaluate code 3 (same effect as evaluate code 1)

  //    for (searchEngine <- Exp3Configs.exp3SearchEngines) {
  //      this.evalExp3CategoryForOneSearchEngine(searchEngine)
  //    }
  //
  //    for (searchEngine <- Exp3Configs.exp3SearchEngines) {
  //      this.evalExp3AmountForOneSearchEngine(searchEngine)
  //    }
  //
  //    for (searchEngine <- Exp3Configs.exp3SearchEngines) {
  //      this.evalExp3ImpactForOneSearchEngine(searchEngine)
  //    }

  //endregion Evaluate code 3 (same effect as evaluate code 1)

  private def evalExp3CategoryForOneSearchEngine(searchEngine: SearchEngine.Value): Unit = {

    val feature: Feature.Value = Feature.Category
    val featureName = feature.toString
    val searchEngineName = searchEngine.toString
    val rnnLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3CategoryPrefix }_${ searchEngineName }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp3Configs.exp3RnnLayer, Exp3Configs.exp3RnnUnitNum)

    val categoryLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3CategoryPrefix }_${ searchEngineName }_Data_${ featureName }.txt")
    val categoryLogLinesAsJava: util.List[String] = Files.lines(categoryLogFile).limit(rnnData.size).collect(Collectors.toList())
    val categoryLogLines: Vector[String] = collectionAsScalaIterable(categoryLogLinesAsJava).toVector

    assert(rnnData.size == categoryLogLines.size)

    val trainData: CategoryTrainData = CategoryTrainData(rnnData, categoryLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp3Configs.exp3CategoryPrefix }_${ searchEngineName }"
    WekaUtils.saveContentToArffFile(trainData, Exp3Configs.exp3PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel(Exp3Configs.exp3Prefix, searchEngineName, feature, Exp3Configs.exp3Algorithm(), Exp3Configs.exp3PathConfig, true)
    model.trainAndCrossValidate()
  }

  private def evalExp3AmountForOneSearchEngine(searchEngine: SearchEngine.Value): Unit = {

    val feature: Feature.Value = Feature.Amount
    val featureName = feature.toString
    val searchEngineName = searchEngine.toString
    val rnnLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3AmountPrefix }_${ searchEngineName }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp3Configs.exp3RnnLayer, Exp3Configs.exp3RnnUnitNum)

    val amountLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3AmountPrefix }_${ searchEngineName }_Data_${ featureName }.txt")
    val amountLogLinesAsJava: util.List[String] = Files.lines(amountLogFile).limit(rnnData.size).collect(Collectors.toList())
    val amountLogLines: Vector[Double] =
      collectionAsScalaIterable(amountLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == amountLogLines.size)

    val trainData: AmountTrainData = AmountTrainData(rnnData, amountLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp3Configs.exp3AmountPrefix }_${ searchEngineName }"
    WekaUtils.saveContentToArffFile(trainData, Exp3Configs.exp3PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel(Exp3Configs.exp3Prefix, searchEngineName, feature, Exp3Configs.exp3Algorithm(), Exp3Configs.exp3PathConfig, true)
    model.trainAndCrossValidate()
  }

  private def evalExp3ImpactForOneSearchEngine(searchEngine: SearchEngine.Value): Unit = {

    val feature: Feature.Value = Feature.Impact
    val featureName = feature.toString
    val searchEngineName = searchEngine.toString
    val rnnLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3ImpactPrefix }_${ searchEngineName }_Data_Summary_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, Exp3Configs.exp3RnnLayer, Exp3Configs.exp3RnnUnitNum)

    val impactLogFile: Path =
      Exp3Configs.exp3PathConfig.trainDataDir.resolve(raw"${ Exp3Configs.exp3ImpactPrefix }_${ searchEngineName }_Data_${ featureName }.txt")
    val impactLogLinesAsJava: util.List[String] = Files.lines(impactLogFile).limit(rnnData.size).collect(Collectors.toList())
    val impactLogLines: Vector[Double] =
      collectionAsScalaIterable(impactLogLinesAsJava)
      .map { _.toDouble }
      .toVector

    assert(rnnData.size == impactLogLines.size)

    val trainData: ImpactTrainData = ImpactTrainData(rnnData, impactLogLines)

    // save arff file to train dir
    val arffFileName: String = s"${ Exp3Configs.exp3ImpactPrefix }_${ searchEngineName }"
    WekaUtils.saveContentToArffFile(trainData, Exp3Configs.exp3PathConfig.trainDataDir, arffFileName)

    val model = new WekaModel(Exp3Configs.exp3Prefix, searchEngineName, feature, Exp3Configs.exp3Algorithm(), Exp3Configs.exp3PathConfig, true)
    model.trainAndCrossValidate()
  }
}
