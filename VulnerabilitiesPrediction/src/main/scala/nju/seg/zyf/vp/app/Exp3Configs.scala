package nju.seg.zyf.vp.app

import java.nio.file.Paths
import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import nju.seg.zyf.vp.Utils
import nju.seg.zyf.vp.Utils.PathConfig
import weka.classifiers.AbstractClassifier
import weka.classifiers.trees.RandomForest

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[app] object Exp3Configs {

  val exp3Prefix: String = "Exp3"
  val exp3CategoryPrefix: String = s"${ Exp3Configs.exp3Prefix }_Category"
  val exp3AmountPrefix: String = s"${ Exp3Configs.exp3Prefix }_Amount"
  val exp3ImpactPrefix: String = s"${ Exp3Configs.exp3Prefix }_Impact"

  val exp3PathConfig: PathConfig = ExpConfigs.createPathConfig(ExpConfigs.baseDir.resolve("Exp3DataDir"))

  val exp3SearchEngines: Seq[SearchEngine.Value] = Seq(SearchEngine.Bing, SearchEngine.Baidu, SearchEngine.Yahoo)

  val exp3Keyword: String = "cross crafted"

  val exp3RnnLayer: Int = ExpConfigs.defaultExpRnnLayer
  val exp3RnnUnitNum: Int = ExpConfigs.defaultExpRnnUnitNum

  val exp3Algorithm: () => AbstractClassifier = () => new RandomForest

  val exp3MinimumSummarySize: Int = 300

  val exp3SkipWhenFileExisted: Boolean = !ExpConfigs.isInTest

  val exp3SelectProductRandomSeed: Long = 0L

  val exp3MaxSelectProductNum: Int = 200

  val exp3DataInstancePerProduct: Int = 5

  @Nonnull @CheckReturnValue
  def selectProducts(products: Vector[String], sizeLimit: Int = this.exp3MaxSelectProductNum): Vector[String] =
    Utils.selectProducts(products, sizeLimit, this.exp3SelectProductRandomSeed)

  def initCrawler(crawler: HtmlCrawler): Unit = ExpConfigs.initCrawler(crawler)
}
