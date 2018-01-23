package nju.seg.zyf.vp.demo

import java.nio.file.Path
import javax.annotation.ParametersAreNonnullByDefault

import crawler.HtmlCrawler
import nju.seg.zyf.vp.HtmlCrawlers.SearchEngine

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
trait IDemoConfig {

  def demoName: String

  def outputPath: Path = DemoConfigs.demoDataDir resolve s"${ this.demoName }"

  def searchEngine: SearchEngine.Value = DemoConfigs.defaultSearchEngine

  def minimumSummarySize: Int = 300

  val skipWhenFileExisted: Boolean = !DemoConfigs.isInTest // only rewrite files when in test

  val dataInstancePerProduct: Int = 10

  def initCrawler(crawler: HtmlCrawler): Unit = DemoConfigs.initCrawler(crawler)
}
