package nju.seg.zyf.vp.app

import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import nju.seg.zyf.vp.ITrainData.Feature
import nju.seg.zyf.vp.Utils
import nju.seg.zyf.vp.Utils.PathConfig
import weka.classifiers.AbstractClassifier
import weka.classifiers.functions.{ LinearRegression, MultilayerPerceptron }
import weka.classifiers.meta.MultiClassClassifier
import weka.classifiers.trees.{ J48, M5P, RandomForest }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[app] object Exp4Configs {

  val exp4Prefix: String = "Exp4"
  val exp4CategoryPrefix: String = s"${ Exp4Configs.exp4Prefix }_Category"
  val exp4AmountPrefix: String = s"${ Exp4Configs.exp4Prefix }_Amount"
  val exp4ImpactPrefix: String = s"${ Exp4Configs.exp4Prefix }_Impact"

  implicit val exp4PathConfig: PathConfig =
    if (ExpConfigs.isInTest) ExpConfigs.testPathConfig
    else ExpConfigs.createPathConfig(ExpConfigs.baseDir.resolve("Exp4DataDir"))

  val exp4AlgorithmsForClassification: Vector[() => AbstractClassifier] = Vector(() => new RandomForest,
                                                                                 () => new MultiClassClassifier,
                                                                                 () => new J48)

  val exp4AlgorithmsForRegression: Vector[() => AbstractClassifier] = Vector(() => new RandomForest,
                                                                             () => new M5P,
                                                                             () => new MultilayerPerceptron,
                                                                             () => new LinearRegression)

  @Nonnull @CheckReturnValue
  def getExp4EvalAlgorithms(feature: Feature.Value): Vector[() => AbstractClassifier] =
    feature match {
      case Feature.Category => this.exp4AlgorithmsForClassification
      case Feature.Amount   => this.exp4AlgorithmsForRegression
      case Feature.Impact   => this.exp4AlgorithmsForRegression
    }

  val exp4AllKeys: Vector[String] = Vector("cross crafted", "")

  val exp4AllKeyCombinations: Vector[String] = this.exp4AllKeys map Utils.whitespaceToUnderscore

  val exp4SearchEngine: SearchEngine.Value = ExpConfigs.defaultSearchEngine

  val exp4RnnLayer: Int = ExpConfigs.defaultExpRnnLayer
  val exp4RnnUnitNum: Int = ExpConfigs.defaultExpRnnUnitNum

  val exp4MinimumSummarySize: Int = 300

  val exp4SkipWhenFileExisted: Boolean = !ExpConfigs.isInTest // only rewrite files when in test

  val exp4SelectProductRandomSeed: Long = 0L

  val exp4MaxSelectProductNum: Int = 200

  val exp4DataInstancePerProduct: Int = 5

  @Nonnull @CheckReturnValue
  def selectProducts(products: Vector[String], sizeLimit: Int = this.exp4MaxSelectProductNum): Vector[String] =
    Utils.selectProducts(products, sizeLimit, this.exp4SelectProductRandomSeed)

  def initCrawler(crawler: HtmlCrawler): Unit = ExpConfigs.initCrawler(crawler)
}
