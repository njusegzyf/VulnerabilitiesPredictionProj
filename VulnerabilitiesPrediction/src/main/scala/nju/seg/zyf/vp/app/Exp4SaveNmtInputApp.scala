package nju.seg.zyf.vp.app

import scala.collection.mutable

import java.io.IOException
import java.nio.file.{ Files, Path }
import javax.annotation.ParametersAreNonnullByDefault

import com.google.common.base.{ Charsets, Strings }

import com.google.common.io.{ Files => GuavaFiles }
import org.slf4j.{ Logger, LoggerFactory }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.{ HtmlCrawlers, SqlUtils, Utils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp4SaveNmtInputApp extends App {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  import ExpConfigs.mySQLConnectionConfig // used as an implicit val
  implicit val crawler: HtmlCrawler = new HtmlCrawler
  Exp4Configs initCrawler this.crawler

  private val allCrawledProductPages: Seq[(String, Vector[(String, String)])] =
    if (ExpConfigs.isInTest) this.getDummyProductPages(Exp4Configs.exp4MaxSelectProductNum)
    else this.crawlProductPages()

  // close crawler at end
  this.crawler.close()

  this.saveProductSummaryAndCategory(allCrawledProductPages = this.allCrawledProductPages,
                                     saveDir = Exp4Configs.exp4PathConfig.trainDataDir,
                                     filePrefix = Exp4Configs.exp4CategoryPrefix,
                                     summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                     skipWhenFileExisted = Exp4Configs.exp4SkipWhenFileExisted)

  this.saveProductSummaryAndAmount(allCrawledProductPages = this.allCrawledProductPages,
                                   saveDir = Exp4Configs.exp4PathConfig.trainDataDir,
                                   filePrefix = Exp4Configs.exp4AmountPrefix,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp4Configs.exp4SkipWhenFileExisted)

  this.saveProductSummaryAndImpact(allCrawledProductPages = this.allCrawledProductPages,
                                   saveDir = Exp4Configs.exp4PathConfig.trainDataDir,
                                   filePrefix = Exp4Configs.exp4ImpactPrefix,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp4Configs.exp4SkipWhenFileExisted)

  private def crawlProductPages(): Seq[(String, Vector[(String, String)])] =
    Exp4Configs.exp4AllKeys map { key =>
      val productAndSummaryTuples: Vector[(String, String)] =
        HtmlCrawlers.crawlProductPages(key,
                                       Exp4Configs.exp4SearchEngine,
                                       Exp4Configs.exp4MaxSelectProductNum,
                                       maxPagePerProduct = Exp4Configs.exp4DataInstancePerProduct,
                                       Exp4Configs.exp4MinimumSummarySize,
                                       Exp4Configs.exp4SelectProductRandomSeed)
      (key, productAndSummaryTuples)
    }

  @throws[IOException]
  private def saveProductSummaryAndCategory(allCrawledProductPages: Seq[(String, Vector[(String, String)])],
                                            saveDir: Path,
                                            filePrefix: String,
                                            summaryTransFunc: String => String,
                                            skipWhenFileExisted: Boolean,
                                            totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForCategoryAndImpact)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndCategoryForOneKeyword(keyword: String,
                                                   productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      this.logger.info(s"Start processing for keyword: $keyword.")
      val keywordCombination = Utils.whitespaceToUnderscore(keyword)

      val summaryFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Summary.txt"
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip keyword: $keywordCombination as the files already existed.")
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
      val categoryFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Category.txt"
      GuavaFiles.asCharSink(categoryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(categories)
    }

    for ((keyword, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndCategoryForOneKeyword(keyword, productAndSummaryTuples)
    }
  }

  @throws[IOException]
  private def saveProductSummaryAndAmount(allCrawledProductPages: Seq[(String, Vector[(String, String)])],
                                          saveDir: Path,
                                          filePrefix: String,
                                          summaryTransFunc: String => String,
                                          skipWhenFileExisted: Boolean,
                                          totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForAmount)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndAmountForOneKeyword(keyword: String,
                                                 productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      this.logger.info(s"Start processing for keyword: $keyword.")
      val keywordCombination = Utils.whitespaceToUnderscore(keyword)

      val summaryFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Summary.txt"
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip keyword: $keywordCombination as the files already existed.")
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
      val amountFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Amount.txt"
      GuavaFiles.asCharSink(amountFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(amounts)
    }

    for ((keyword, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndAmountForOneKeyword(keyword, productAndSummaryTuples)
    }
  }

  @throws[IOException]
  private def saveProductSummaryAndImpact(allCrawledProductPages: Seq[(String, Vector[(String, String)])],
                                          saveDir: Path,
                                          filePrefix: String,
                                          summaryTransFunc: String => String,
                                          skipWhenFileExisted: Boolean,
                                          totalInstanceLimit: Int = ExpConfigs.defaultInstanceLimitForCategoryAndImpact)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))
    require(summaryTransFunc != null)

    def saveProductSummaryAndImpactForOneKeyword(keyword: String,
                                                 productAndSummaryTuples: Vector[(String, String)])
    : Unit = {
      this.logger.info(s"Start processing for keyword: $keyword.")
      val keywordCombination = Utils.whitespaceToUnderscore(keyword)

      val summaryFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Summary.txt"
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip keyword: $keywordCombination as the files already existed.")
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
      val impactFile: Path = saveDir resolve s"${ filePrefix }_${ keywordCombination }_Data_Impact.txt"
      GuavaFiles.asCharSink(impactFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(impacts)
    }

    for ((keyword, productAndSummaryTuples) <- allCrawledProductPages) {
      saveProductSummaryAndImpactForOneKeyword(keyword, productAndSummaryTuples)
    }
  }

  /** For tests only. */
  //noinspection ScalaUnusedSymbol,ScalaDeprecation
  @deprecated
  private def getDummyProductPages(maxSelectProductNum: Int): Seq[(String, Vector[(String, String)])] = {
    Exp4Configs.exp4AllKeys map { (keyword: String) =>
      assert(keyword != null)

      val selectedProductsWithVersion: Vector[String] =
        Utils.selectProducts(SqlUtils.getValidProductNamesWithVersion(), maxSelectProductNum, Exp4Configs.exp4SelectProductRandomSeed)

      // dummy content
      val dummyContent: Vector[String] = SqlUtils.getDummySummaries

      val productAndSummaryTuples: Vector[(String, String)] =
        selectedProductsWithVersion.flatMap { Iterable.fill(5)(_) }
        .zip(dummyContent)
        .map { tuple => (tuple._1, tuple._2) }

      (keyword, productAndSummaryTuples)
    }
  }
}
