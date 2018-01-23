package crawler

import scala.util.{ Failure, Success, Try }

import java.util.logging.Level
import javax.annotation.{ CheckReturnValue, Nonnull }
import javax.annotation.concurrent.NotThreadSafe

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html._

import org.apache.commons.logging.LogFactory

/**
  * Created by ReggieYang on 2017/1/8.
  *
  * @version 0.1 Fix init logic.
  * @author Zhang Yifan
  */
@NotThreadSafe
final class HtmlCrawler extends Serializable {

  var siteXpathMap: Map[String, String] = _
  var webClient: WebClient = _

  /** @since 0.1 */
  var initFunc: () => Unit = _

  /** @since 0.1 */
  var webClientExceptionCount = 0

  /**
    * @since 0.1
    */
  def initWithProxy(useSocksProxy: Boolean = true,
                    proxyHost: String = "127.0.0.1",
                    proxyPort: Int = 1080)
  : Unit = {
    LogFactory.getFactory.setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF)
    java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF)

    // record the init func for reinitialize
    this.initFunc = () => {
      this.webClient = new WebClient()
      this.webClient.getOptions.setCssEnabled(false)
      this.webClient.getOptions.setJavaScriptEnabled(false)

      if (useSocksProxy) {
        this.webClient.getOptions.getProxyConfig.setSocksProxy(useSocksProxy)
        this.webClient.getOptions.getProxyConfig.setProxyHost(proxyHost)
        this.webClient.getOptions.getProxyConfig.setProxyPort(proxyPort)
      }
    }

    // init web client
    this.initFunc()
  }

  /**
    * @since 0.1
    */
  def reinitialize(): Unit = {
    assert(this.initFunc != null)

    this.initFunc()
  }

  // FIXME Old code still call this method to init.

  @deprecated("", "0.1")
  def init(): Unit = {
    this.initWithProxy(useSocksProxy = true,
                       proxyHost = "127.0.0.1",
                       proxyPort = 1080)
  }

  def close(): Unit = webClient.close()

  def getSiteXpathMap: Map[String, String] = {
    val delimiter = "\t"
    scala.io.Source.fromFile("data\\crawler\\site_xpath")
    .getLines()
    .map(line => {
      (line.split(delimiter)(0), line.split(delimiter)(1))
    }).toMap
  }

  def crawlDescription(url: String): String = {
    val domain = url.replaceAll("((?:http|ftp)://.*?)/.*", "$1")
    //利用正则表达式从完整的url中获得网站的域名
    siteXpathMap.get(domain) match {
      //从siteXpathMap中根据网站域名得到待提取内容对应的Xpath
      case None        => url
      case Some(xpath) =>
        var page: HtmlPage = null
        try {
          page = webClient.getPage(url)
        }
        catch {
          case (e: Exception) => e.printStackTrace()
          case _: Throwable   =>
        }
        val newXpath = xpath.split("&")(0)
        val attribute = if (xpath.contains("&")) xpath.split("&")(1) else ""

        if (page == null) url
        else {
          val des = page.getByXPath(newXpath).get(0)
          des match {
            case des: DomElement => getContent(des, attribute)
            //利用XPath得到描述信息所在的标签，如果有必要，提取所需的属性，否则直接提取内容
            case _ => des.toString
          }
        }
    }
  }

  def getContent(ele: DomElement, attr: String): String = if (attr.isEmpty) ele.asText() else ele.getAttribute(attr)

  def getBaiduRes(kw: String): Array[String] = {
    val kw2 = kw.replaceAll(" ", "+")
    val page: HtmlPage = webClient.getPage(s"https://www.baidu.com/s?wd=$kw2&sl_lang=en&rsv_srlang=en&rsv_rq=en")
    page.getByXPath("//h3[@class='t c-title-en']//a").toArray().map(_.asInstanceOf[HtmlAnchor].getHrefAttribute)
  }

  def getYahooRes(kw: String): Array[String] = {
    val kw2 = kw.replaceAll(" ", "+")
    val page: HtmlPage = webClient.getPage(s"https://search.yahoo.com/search?p=$kw2")
    page.getByXPath("//h3[@class='title']//a").toArray().map(_.asInstanceOf[HtmlAnchor].getHrefAttribute)
  }

  def getBingRes(kw: String): Array[String] = {
    val kw2 = kw.replaceAll(" ", "+")
    val page: HtmlPage = webClient.getPage(s"http://global.bing.com/search?q=$kw2&go=Search&qs=bs&setmkt=en-us&setlang=en-us&FORM=SECNEN")
    page.getByXPath("//li[@class='b_algo']//h2//a").toArray().map(_.asInstanceOf[HtmlAnchor].getHrefAttribute)
  }

  def getGoogleRes(kw: String): Array[String] = {
    val kw2 = kw.replaceAll(" ", "+")
    val page: HtmlPage = webClient.getPage(s"https://www.google.com/search?q=$kw2&lr=lang_en")
    page.getByXPath("//div[@class='rc']//h3//a").toArray().map(_.asInstanceOf[HtmlAnchor].getHrefAttribute)
  }

  @deprecated("Wrong error handling. Using `tryCrawlPage` instead.", "0.1")
  def crawlPage(url: String): String = {
    println(s"crawling page: $url")
    try {
      //      val urlWithProtocol = if (url.startsWith("http")) url else s"http://$url"
      val page: HtmlPage = webClient.getPage(url)
      if (page == null || page.getBody == null || page.getBody.asText() == null) null
      else page.getBody.asText().replaceAll("\\s+", " ")
    }
    catch {
      case e: Throwable =>
        e.printStackTrace()

        this.handleWebClientException(e)
        // FIXME It is wrong to return an empty string here
        ""
    }
  }

  @deprecated("0.1", "Wrong error handling. Using `tryCrawlPage` instead.")
  def crawlPage(urls: Array[String], length: Int = -1): Array[String] = {
    val urlRegex = ".*".r
    val validUrls = urls.filter(url => urlRegex.findFirstIn(url).nonEmpty)

    val urlsLen = if (length < 0) validUrls
                  else if (length <= validUrls.length) validUrls.take(length)
                  else urls ++ Array.fill(length - validUrls.length)("")

    urlsLen.map(url => {
      println(s"crawling page: $url")
      Try {
        val urlWithProtocol = if (url.startsWith("http")) url else s"http://$url"
        val page: HtmlPage = webClient.getPage(urlWithProtocol)
        page
      } match {
        case Success(p) =>
          if (p == null || p.getBody == null || p.getBody.asText() == null) null
          else p.getBody.asText().replaceAll("\\s+", " ")
        case Failure(f) =>
          f.printStackTrace()
          null
      }
    }).filter(_ != null)
  }

  /** @since 0.1 */
  @Nonnull @CheckReturnValue
  def tryCrawlPage(url: String): Option[String] = {
    println(s"crawling page: $url")
    try {
      //      val urlWithProtocol = if (url.startsWith("http")) url else s"http://$url"
      val page: HtmlPage = webClient.getPage(url)
      if (page == null || page.getBody == null || page.getBody.asText() == null) Option.empty
      else Option(page.getBody.asText().replaceAll("\\s+", " "))
    }
    catch {
      case e: Throwable =>
        e.printStackTrace()

        this.handleWebClientException(e)

        Option.empty
    }
  }

  private def handleWebClientException(ignored: Throwable): Unit = {
    // FIXME We may need to do some operations when the web client throws too much exceptions
    this.webClientExceptionCount += 1
    this.webClient.close()
    this.reinitialize()
  }
}
