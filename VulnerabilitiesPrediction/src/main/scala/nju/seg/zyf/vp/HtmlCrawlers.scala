package nju.seg.zyf.vp

import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Strings

import crawler.HtmlCrawler
import nju.seg.zyf.vp.SqlUtils.DbConnectionConfig
import nju.seg.zyf.vp.app.{ Exp3Configs, ExpConfigs }
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object HtmlCrawlers {

  object SearchEngine extends Enumeration {
    val Google = Value("Google")
    val Baidu = Value("Baidu")
    val Bing = Value("Bing")
    val Yahoo = Value("Yahoo")
  }

  implicit final class RichHtmlCrawler(val crawler: HtmlCrawler) {
    require(this.crawler != null)

    @Nonnull @CheckReturnValue
    def getRes(@Nonnull keyWord: String)
              (implicit searchEngine: SearchEngine.Value)
    : Array[String] = {
      require(!Strings.isNullOrEmpty(keyWord))
      require(searchEngine != null)

      searchEngine match {
        case SearchEngine.Baidu  => this.crawler.getBaiduRes(keyWord)
        case SearchEngine.Bing   => this.crawler.getBingRes(keyWord)
        case SearchEngine.Google => this.crawler.getGoogleRes(keyWord)
        case SearchEngine.Yahoo  => this.crawler.getYahooRes(keyWord)
      }
    }
  }

  /** Crawls product pages for a given key word using given search engines. */
  @Nonnull @CheckReturnValue
  def crawlProductPages(keyWord: String,
                        searchEngine: SearchEngine.Value,
                        maxSelectProductNum: Int,
                        maxPagePerProduct: Int,
                        minPageLen: Int,
                        selectProductRandomSeed: Long)
                       (implicit crawler: HtmlCrawler,
                        dbConnectionConfig: DbConnectionConfig)
  : Vector[(String, String)] = {
    require(keyWord != null) // require(!Strings.isNullOrEmpty(keyWord))
    require(searchEngine != null)
    require(maxSelectProductNum > 0)
    require(maxPagePerProduct > 0)
    require(minPageLen > 0)
    require(crawler != null)
    require(dbConnectionConfig != null)

    assert(searchEngine != null)
    implicit val implicitSearchEngine: SearchEngine.Value = searchEngine

    val selectedProductsWithVersion: Vector[String] = Utils.selectProducts(SqlUtils.getValidProductNamesWithVersion(),
                                                                           maxSelectProductNum,
                                                                           selectProductRandomSeed)

    val productAndSummaryTuples: Vector[(String, String)] =
      selectedProductsWithVersion flatMap { product =>
        RichHtmlCrawler(crawler).getRes(s"$product $keyWord").view
        .filter { this.filterCrawPage }
        .take(maxPagePerProduct) // limit pages for one product
        .map { crawler.tryCrawlPage }
        .filter { contentOption => contentOption.isDefined && contentOption.get.length >= minPageLen }
        .map { contentOption => (product, contentOption.get) }
      }

    productAndSummaryTuples
  }

  /** Crawls product pages for a given key word using given search engines. */
  @Nonnull @CheckReturnValue
  def crawlSpecifiedProductPages(productAndKeyWord: String,
                                 searchEngine: SearchEngine.Value,
                                 maxPagePerProduct: Int,
                                 minPageLen: Int)
                                (implicit crawler: HtmlCrawler,
                                 dbConnectionConfig: DbConnectionConfig)
  : Vector[(String, String)] = {
    require(productAndKeyWord != null)
    require(searchEngine != null)
    require(maxPagePerProduct > 0)
    require(minPageLen > 0)
    require(crawler != null)
    require(dbConnectionConfig != null)

    assert(searchEngine != null)
    implicit val implicitSearchEngine: SearchEngine.Value = searchEngine

    val urlAndSummaryTuples: Vector[(String, String)] =
      RichHtmlCrawler(crawler).getRes(productAndKeyWord).view
      .filter { this.filterCrawPage }
      .map { url => val optionalContent = crawler tryCrawlPage url; (url, optionalContent) }
      .filter { data => data._2.isDefined && data._2.get.length >= minPageLen }
      .take(maxPagePerProduct) // limit pages for one product
      .map { data => (data._1, data._2.get) }
      .toVector

    urlAndSummaryTuples
  }

  def filterCrawPage(pageUrl: String): Boolean = {
    if (Strings.isNullOrEmpty(pageUrl)) false
    else if (pageUrl.contains("facebook")) false
    else if (pageUrl.endsWith("xml")) false
    else true
  }
}
