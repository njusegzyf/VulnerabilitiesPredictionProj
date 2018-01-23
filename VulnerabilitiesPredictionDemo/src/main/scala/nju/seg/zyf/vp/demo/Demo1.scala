package nju.seg.zyf.vp.demo

import scala.collection.mutable
import scala.language.postfixOps

import java.io.{ FileOutputStream, ObjectOutputStream }
import java.nio.file.Path
import javax.annotation.ParametersAreNonnullByDefault

import org.slf4j.{ Logger, LoggerFactory }

import nju.seg.zyf.vp.ITrainData.Feature
import weka.classifiers.Classifier
import weka.core.{ Instances, SerializationHelper }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Demo1 extends IDemo {

  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected val config: IDemoConfig = Demo1Config

  override protected val productName : String = "Acrobat 9.4.1"

  override protected val productNameAndKeyword : String= s"${this.productName} Cross Crafted"

  private val crawledProductPages: (String, Vector[(String, String)]) =
    if (!DemoConfigs.isInTest) this.crawlProductPages()
    else this.getDummyProductPages()

  def main(args: Array[String]): Unit = {

    if (!DemoConfigs.isInTest) {
      val pagesSaveFilePath: Path = this.config.outputPath resolve "CrawledProductPages"
      val outObj = new ObjectOutputStream(new FileOutputStream(pagesSaveFilePath.toFile))
      outObj.writeObject(crawledProductPages)
      outObj.close()
    }

    // save crawled product pages
    this.saveProductSummaries(this.crawledProductPages._1,
                              this.crawledProductPages._2,
                              this.config.outputPath,
                              DemoConfigs.defaultSentenceTransFunc,
                              this.config.skipWhenFileExisted)

    // use nmt to convert product summaries to semantic encoding vectors
    import scala.sys.process._
    val scriptsPath = DemoConfigs.demoScriptDir
    s"powershell $scriptsPath/Start-NmtInfer.ps1 -vpDemoName ${this.config.demoName}"!

    // convert to Weka input
    val instances : Instances = this.convertToWekaInstance(Feature.Amount)

    // load classifier
    val classifierFile = DemoConfigs.demoWekaModelDir resolve "Exp3_Amount_Baidu_RandomForest.cls"
    val classifier: Classifier = SerializationHelper.read(classifierFile.toString).asInstanceOf[Classifier]

    // use classifier
    import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`
    val instanceValues: mutable.Buffer[Double] =
      for (instance <- instances)
        yield classifier.classifyInstance(instance)

    val averageInstanceValue = instanceValues.sum / instanceValues.length
    println(s"The predicate vulnerability amount of ${this.productName} is : $averageInstanceValue")
  }

}
