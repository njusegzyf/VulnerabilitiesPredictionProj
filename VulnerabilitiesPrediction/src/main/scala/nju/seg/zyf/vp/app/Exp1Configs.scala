package nju.seg.zyf.vp.app

import java.nio.file.Paths
import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import nju.seg.zyf.vp.ITrainData.Feature

import nju.seg.zyf.vp.Utils.PathConfig
import weka.classifiers.AbstractClassifier
import weka.classifiers.functions.{ LinearRegression, MultilayerPerceptron }
import weka.classifiers.meta.MultiClassClassifier
import weka.classifiers.trees.{ J48, M5P, RandomForest }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[vp] object Exp1Configs {

  val exp1Prefix: String = "Exp1"
  val exp1CategoryPrefix: String = s"${ Exp1Configs.exp1Prefix }_Category"
  val exp1AmountPrefix: String = s"${ Exp1Configs.exp1Prefix }_Amount"
  val exp1ImpactPrefix: String = s"${ Exp1Configs.exp1Prefix }_Impact"

  val exp1PathConfig: PathConfig = ExpConfigs.createPathConfig(ExpConfigs.baseDir.resolve("Exp1DataDir"))

  val exp1AlgorithmsForClassification: Vector[() => AbstractClassifier] = Vector(() => new RandomForest,
                                                                                 () => new MultiClassClassifier,
                                                                                 () => new J48)

  val exp1AlgorithmsForRegression: Vector[() => AbstractClassifier] = Vector(() => new RandomForest,
                                                                             () => new M5P,
                                                                             () => new MultilayerPerceptron,
                                                                             () => new LinearRegression)

  /** Algorithms that take a long time and thus should only be used in a limit data set. */
  val exp1LimitedAlgorithmsForClassification: Vector[() => AbstractClassifier] = Vector(() => new MultiClassClassifier)

  val exp1LimitedAlgorithmsForRegression: Vector[() => AbstractClassifier] = Vector(() => new MultilayerPerceptron)

  @Nonnull @CheckReturnValue
  def getExp1EvalAlgorithms(feature: Feature.Value): Vector[() => AbstractClassifier] =
    feature match {
      case Feature.Category => this.exp1AlgorithmsForClassification
      case Feature.Amount   => this.exp1AlgorithmsForRegression
      case Feature.Impact   => this.exp1AlgorithmsForRegression
    }

  @Nonnull @CheckReturnValue
  def getExp1EvalLimitedAlgorithms(feature: Feature.Value): Vector[() => AbstractClassifier] =
    feature match {
      case Feature.Category => this.exp1LimitedAlgorithmsForClassification
      case Feature.Amount   => this.exp1LimitedAlgorithmsForRegression
      case Feature.Impact   => this.exp1LimitedAlgorithmsForRegression
    }

  val exp1RnnLayer: Int = ExpConfigs.defaultExpRnnLayer
  val exp1RnnUnitNum: Int = ExpConfigs.defaultExpRnnUnitNum

  val exp1DataInstancesSizeLimit: Int = 10000

  val exp1DataInstancesSizeLimitForLimitedAlgorithms: Int = 2000

}
