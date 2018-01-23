package nju.seg.zyf.vp.demo

import java.nio.file.{ Path, Paths }
import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.{ RnnUtils, Utils }
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import nju.seg.zyf.vp.SqlUtils.DbConnectionConfig
import nju.seg.zyf.vp.Utils.PathConfig
import nju.seg.zyf.vp.app.ExpConfigs

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[vp] object DemoConfigs {

  val isInTest: Boolean = true

  @Nonnull
  val mySqlConnectionProperties: String = raw"useUnicode=true&characterEncoding=utf-8&useSSL=false"

  @Nonnull
  val mySqlConnectionString: String = raw"jdbc:mysql://localhost:3306/nvd?${ this.mySqlConnectionProperties }"

  @Nonnull
  implicit val mySQLConnectionConfig: DbConnectionConfig = DbConnectionConfig(DemoConfigs.mySqlConnectionString,
                                                                              user = "root",
                                                                              password = "root")

  @Nonnull
  val demoProjDir: Path = Paths.get("C:\\IDEAProjs\\VulnerabilitiesPredictionProj\\VulnerabilitiesPredictionDemo")

  @Nonnull
  val demoScriptDir : Path = this.demoProjDir resolve "src/main/resources"

  @Nonnull
  val demoDataDir: Path = this.demoProjDir resolve "data"

  @Nonnull
  val demoWekaModelDir: Path = this.demoDataDir resolve "ModelDir"

  val defaultExpMinimumAmount: Double = 100.0

  @Nonnull
  val amountFilter: Double => Boolean = { _ >= this.defaultExpMinimumAmount }

  val defaultRnnLayer: Int = 2 // ExpConfigs.defaultExpRnnLayer
  val defaultRnnUnitNum: Int = 128 // ExpConfigs.defaultExpRnnUnitNum

  @Nonnegative
  val defaultExpMinimumSummarySize: Int = 100

  val sentenceLengthLimit: Int = 5000

  val defaultSentenceTransFunc: String => String = { RnnUtils.summaryToSentence(_, DemoConfigs.sentenceLengthLimit) }

  val defaultSearchEngine: SearchEngine.Value = SearchEngine.Baidu

  def initCrawler(crawler: HtmlCrawler): Unit = {
    require(crawler != null)

    crawler.initWithProxy(useSocksProxy = false)
  }
}
