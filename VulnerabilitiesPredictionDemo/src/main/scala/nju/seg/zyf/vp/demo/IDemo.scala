package nju.seg.zyf.vp.demo

import java.util
import java.io.{ FileInputStream, IOException, ObjectInputStream }
import java.nio.file.{ Files, Path }
import java.util.stream.Collectors
import javax.annotation.ParametersAreNonnullByDefault

import com.google.common.base.Charsets

import org.slf4j.{ Logger, LoggerFactory }

import crawler.HtmlCrawler
import nju.seg.zyf.vp.{ HtmlCrawlers, ITrainData, RnnUtils, Utils, WekaUtils }
import nju.seg.zyf.vp.ITrainData.{ AmountTrainData, Feature, ImpactTrainData }
import nju.seg.zyf.vp.RnnUtils.EncoderStateData
import weka.core.Instances
import weka.core.converters.ConverterUtils.DataSource

import com.google.common.io.{Files => GuavaFiles}

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
trait IDemo {

  protected def logger: Logger

  protected def config: IDemoConfig

  protected def productName : String

  protected def productNameAndKeyword : String

  @throws[IOException]
  protected def saveProductSummaries(productKeyword: String,
                                   crawledProductPages: Vector[(String, String)],
                                   saveDir: Path,
                                   summaryTransFunc: String => String,
                                   skipWhenFileExisted: Boolean)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(summaryTransFunc != null)

    this.logger.info(s"Save product summaries for keyword: $productKeyword.")
    val keywordCombination = Utils.whitespaceToUnderscore(productKeyword)

    val summaryFile: Path = saveDir resolve s"${ this.config.demoName }_Data_Summary.txt"
    if (skipWhenFileExisted && Files.exists(summaryFile)) {
      this.logger.info(s"Skip keyword: $keywordCombination as the files already existed.")
      return
    }

    val summaries = crawledProductPages map { _._2 }

    // write summaries to the file as nmt infer input file
    import scala.collection.JavaConverters.asJavaIterable
    GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)
  }

  @throws[IOException]
  protected final def convertToWekaInstance(featureType : Feature.Value) : Instances = {
    val rnnLogFile: Path = this.config.outputPath.resolve(raw"${ this.config.demoName }_Infer_Err_UTF8.txt")
    import scala.collection.JavaConverters.collectionAsScalaIterable
    val rnnLogLinesAsJava: util.List[String] = Files.lines(rnnLogFile).collect(Collectors.toList())
    val rnnLogLines: Iterable[String] = collectionAsScalaIterable(rnnLogLinesAsJava)
    val rnnData: EncoderStateData[Double] = RnnUtils.parseRnnLogForce(rnnLogLines, DemoConfigs.defaultRnnLayer, DemoConfigs.defaultRnnUnitNum)

    val notUsedDataClasses: Vector[Double] = Vector.fill(rnnData.size) { 0.0 }

    assert(rnnData.size == notUsedDataClasses.size)

    val trainData : ITrainData[_, _] =
      featureType match {
        case Feature.Amount => AmountTrainData(rnnData, notUsedDataClasses)
        case Feature.Impact => ImpactTrainData(rnnData, notUsedDataClasses)
      }

    // save arff file to train dir
    val arffFileName: String = s"${ this.config.demoName }_WekaInstances"
    WekaUtils.saveContentToArffFile(trainData, this.config.outputPath, arffFileName)

    val trainDataFile: Path = this.config.outputPath resolve s"$arffFileName.arff"
    val instances: Instances = DataSource.read(trainDataFile.toString)
    // take the lass attribute as class data
    val classIndex: Int = instances.numAttributes() - 1
    instances.setClassIndex(classIndex)

    instances
  }

  protected final def crawlProductPages(): (String, Vector[(String, String)]) = {

    import DemoConfigs.mySQLConnectionConfig // used as an implicit val
    implicit val crawler: HtmlCrawler = new HtmlCrawler
    this.config initCrawler crawler

    val urlAndSummaryTuples: Vector[(String, String)] =
      HtmlCrawlers.crawlSpecifiedProductPages(this.productNameAndKeyword,
                                              this.config.searchEngine,
                                              maxPagePerProduct = this.config.dataInstancePerProduct,
                                              this.config.minimumSummarySize)

    // close crawler at end
    crawler.close()

    (this.productNameAndKeyword, urlAndSummaryTuples)
  }

  protected final def getDummyProductPages(): (String, Vector[(String, String)]) = {
    val pagesSaveFilePath: Path = this.config.outputPath resolve "CrawledProductPages"
    val in = new ObjectInputStream(new FileInputStream(pagesSaveFilePath.toFile))
    val obj = in.readObject()
    in.close()

    val dummyProductPages = obj.asInstanceOf[(String, Vector[(String, String)])]
    for (page <- dummyProductPages._2) {
      println(s"Read pre crawled page: ${ page._1 }")
    }

    dummyProductPages
  }
}
