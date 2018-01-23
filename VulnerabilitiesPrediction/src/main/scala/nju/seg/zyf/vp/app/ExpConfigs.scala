package nju.seg.zyf.vp.app

import java.nio.file.{ Path, Paths }
import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import nju.seg.zyf.vp.SqlUtils.DbConnectionConfig
import nju.seg.zyf.vp.Utils.PathConfig
import nju.seg.zyf.vp.{ RnnUtils, Utils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[vp] object ExpConfigs {

  val isInTest: Boolean = false

  @Nonnull
  val mySqlConnectionProperties: String = raw"useUnicode=true&characterEncoding=utf-8&useSSL=false"

  @Nonnull
  val mySqlConnectionString: String = raw"jdbc:mysql://localhost:3306/nvd?${ this.mySqlConnectionProperties }"

  @Nonnull
  implicit val mySQLConnectionConfig: DbConnectionConfig = DbConnectionConfig(ExpConfigs.mySqlConnectionString,
                                                                              user = "root",
                                                                              password = "root")

  @Nonnull
  val testDir: Path = Paths.get("E:/testDir")

  @Nonnull
  val testPathConfig: PathConfig = ExpConfigs.createPathConfig(ExpConfigs.testDir)

  @Nonnull
  val baseDir: Path = Paths.get("E:/RNN")

  @Nonnull @CheckReturnValue
  def createPathConfig(baseDir: Path): PathConfig = {
    Utils.requireIsDirOrCanMakeDir(baseDir)

    PathConfig(trainDataDir = baseDir resolve "trainData",
               modelDir = baseDir resolve "model",
               evaluationDir = baseDir resolve "evaluation",
               wekaMiddleResultDir = baseDir resolve "wekaMiddleResult",
               wekaMiddleResultRecordDir = baseDir resolve "wekaMiddleResultRecord")
  }

  val defaultExpMinimumAmount: Double = 100.0

  @Nonnull
  val amountFilter: Double => Boolean = { _ >= this.defaultExpMinimumAmount }

  @Nonnegative
  val defaultExpMinimumSummarySize: Int = 100

  val defaultExpRnnLayer: Int = 2
  val defaultExpRnnUnitNum: Int = 128

  val sentenceLengthLimit: Int = 5000

  val defaultSentenceTransFunc: String => String = { RnnUtils.summaryToSentence(_, ExpConfigs.sentenceLengthLimit) }

  val defaultSearchEngine: SearchEngine.Value = SearchEngine.Bing

  val defaultInstanceLimitForAmount : Int = 120

  val defaultInstanceLimitForCategoryAndImpact : Int = 400

  def initCrawler(crawler: HtmlCrawler): Unit = {
    require(crawler != null)

    crawler.initWithProxy(useSocksProxy = true,
                          proxyHost = "114.212.87.92",
                          proxyPort = 1080)
  }
}
