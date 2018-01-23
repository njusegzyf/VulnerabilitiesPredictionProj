package nju.seg.zyf.vp.app

import scala.collection.mutable

import java.io.IOException
import java.nio.file.{ Files, Path, Paths }
import javax.annotation.ParametersAreNonnullByDefault

import com.google.common.base.{ Charsets, Strings }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.{ HtmlCrawlers, SqlUtils, Utils }
import com.google.common.io.{ Files => GuavaFiles }

import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine
import org.slf4j.{ Logger, LoggerFactory }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp3SaveNmtInputApp extends App {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  import ExpConfigs.mySQLConnectionConfig // used as an implicit val
  implicit val crawler: HtmlCrawler = new HtmlCrawler
  Exp3Configs.initCrawler(this.crawler)

  private val allCrawledProductPages: Seq[(HtmlCrawlers.SearchEngine.Value, Vector[(String, String)])] =
    if (ExpConfigs.isInTest) this.getDummyProductPages(Exp3Configs.exp3MaxSelectProductNum)
    else this.crawlProductPages()

  this.saveProductSummaryAndCategory(allCrawledProductPages = this.allCrawledProductPages,
                                     saveDir = Exp3Configs.exp3PathConfig.trainDataDir,
                                     filePrefix = Exp3Configs.exp3CategoryPrefix,
                                     summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                     skipWhenFileExisted = Exp3Configs.exp3SkipWhenFileExisted)

  this.saveProductSummaryAndAmount(allCrawledProductPages = this.allCrawledProductPages,
                                   saveDir = Exp3Configs.exp3PathConfig.trainDataDir,
                                   filePrefix = Exp3Configs.exp3AmountPrefix,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp3Configs.exp3SkipWhenFileExisted)

  this.saveProductSummaryAndImpact(allCrawledProductPages = this.allCrawledProductPages,
                                   saveDir = Exp3Configs.exp3PathConfig.trainDataDir,
                                   filePrefix = Exp3Configs.exp3ImpactPrefix,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp3Configs.exp3SkipWhenFileExisted)

  // close crawler at end
  this.crawler.close()

  private def crawlProductPages(): Seq[(SearchEngine.Value, Vector[(String, String)])] =
    Exp3Configs.exp3SearchEngines map { searchEngine =>
      val productAndSummaryTuples: Vector[(String, String)] =
        HtmlCrawlers.crawlProductPages(Exp3Configs.exp3Keyword,
                                       searchEngine,
                                       Exp3Configs.exp3MaxSelectProductNum,
                                       maxPagePerProduct = Exp3Configs.exp3DataInstancePerProduct,
                                       Exp3Configs.exp3MinimumSummarySize,
                                       Exp3Configs.exp3SelectProductRandomSeed)
      (searchEngine, productAndSummaryTuples)
    }

  @throws[IOException]
  def saveProductSummaryAndCategory(allCrawledProductPages: Seq[(SearchEngine.Value, Vector[(String, String)])],
                                    saveDir: Path,
                                    filePrefix: String,
                                    summaryTransFunc: String => String,
                                    skipWhenFileExisted: Boolean,
                                    totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForCategoryAndImpact)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndCategoryForOneSearchEngine(searchEngine: SearchEngine.Value,
                                                        productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      val searchEngineName = searchEngine.toString
      this.logger.info(s"Start processing with search engine: $searchEngineName.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip search engine: $searchEngineName as the files already existed.")
        return
      }

      val productToCategoryMap: mutable.HashMap[String, String] = SqlUtils.getProductToCategoryMap
      val tuples: Seq[(String, String)] =
        productAndSummaryTuples.view
        .take(totalInstanceLimit)
        .map { ps =>
          val (product, summary) = ps
          (summary, productToCategoryMap(product))
        }.force

      val summaries = tuples map { _._1 }
      val categories = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write categories to the file
      val categoryFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Category.txt")
      GuavaFiles.asCharSink(categoryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(categories)
    }

    for ((searchEngine, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndCategoryForOneSearchEngine(searchEngine, productAndSummaryTuples)
    }
  }

  @throws[IOException]
  def saveProductSummaryAndAmount(allCrawledProductPages: Seq[(SearchEngine.Value, Vector[(String, String)])],
                                  saveDir: Path,
                                  filePrefix: String,
                                  summaryTransFunc: String => String,
                                  skipWhenFileExisted: Boolean,
                                  totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForAmount)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndAmountForOneSearchEngine(searchEngine: SearchEngine.Value,
                                                      productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      val searchEngineName = searchEngine.toString
      this.logger.info(s"Start processing with search engine: $searchEngineName.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip search engine: $searchEngineName as the files already existed.")
        return
      }

      val productToAmountMap: mutable.HashMap[String, String] = SqlUtils.getProductToAmountMap
      val tuples: Seq[(String, String)] =
        productAndSummaryTuples.view
        .take(totalInstanceLimit)
        .map { ps =>
          val (product, summary) = ps
          (summary, productToAmountMap(product))
        }.force

      val summaries = tuples map { _._1 }
      val amounts = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write amounts to the file
      val amountFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Amount.txt")
      GuavaFiles.asCharSink(amountFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(amounts)
    }

    for ((searchEngine, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndAmountForOneSearchEngine(searchEngine, productAndSummaryTuples)
    }
  }

  @throws[IOException]
  def saveProductSummaryAndImpact(allCrawledProductPages: Seq[(SearchEngine.Value, Vector[(String, String)])],
                                  saveDir: Path,
                                  filePrefix: String,
                                  summaryTransFunc: String => String,
                                  skipWhenFileExisted: Boolean,
                                  totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForCategoryAndImpact)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndImpactForOneSearchEngine(searchEngine: SearchEngine.Value,
                                                      productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      val searchEngineName = searchEngine.toString
      this.logger.info(s"Start processing with search engine: $searchEngineName.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip search engine: $searchEngineName as the files already existed.")
        return
      }

      val productToImpactMap: mutable.HashMap[String, String] = SqlUtils.getProductToImpactMap
      val tuples: Seq[(String, String)] =
        productAndSummaryTuples.view
        .take(totalInstanceLimit)
        .map { ps =>
          val (product, summary) = ps
          (summary, productToImpactMap(product))
        }.force

      val summaries = tuples map { _._1 }
      val impacts = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write impacts to the file
      val impactFile: Path = saveDir.resolve(s"${ filePrefix }_${ searchEngineName }_Data_Impact.txt")
      GuavaFiles.asCharSink(impactFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(impacts)
    }

    for ((searchEngine, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndImpactForOneSearchEngine(searchEngine, productAndSummaryTuples)
    }
  }

  /** For tests only. */
  //noinspection ScalaUnusedSymbol,ScalaDeprecation
  @deprecated
  private def getDummyProductPages(maxSelectProductNum: Int): Seq[(SearchEngine.Value, Vector[(String, String)])] = {
    Exp3Configs.exp3SearchEngines map { (searchEngine: SearchEngine.Value) =>
      assert(searchEngine != null)
      implicit val implicitSearchEngine = searchEngine

      val selectedProductsWithVersion: Vector[String] =
        Exp3Configs.selectProducts(SqlUtils.getValidProductNamesWithVersion(), maxSelectProductNum)

      // dummy content
      val dummyContent: Vector[String] = SqlUtils.getDummySummaries

      val productAndSummaryTuples: Vector[(String, String)] =
        selectedProductsWithVersion.flatMap { Iterable.fill(5)(_) }
        .zip(dummyContent)
        .map { tuple => (tuple._1, tuple._2) }

      (searchEngine, productAndSummaryTuples)
    }
  }
}
