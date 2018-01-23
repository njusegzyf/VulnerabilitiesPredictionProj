package workflow

import java.io.PrintWriter
import java.sql.Connection

import nvd.data.{ DBConnection, NvdItemDao, SummaryExtraction }
import org.slf4j.{ Logger, LoggerFactory }

import util.{ ResultSetIt, ResultSetIt2 }
import java.util

import crawler.HtmlCrawler
import nvd.model.ProductSearch
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory

import weka.ModelTrain
import weka.classifiers.trees.RandomForest
import weka.core.converters.ConverterUtils.DataSink
import weka.core.{ Attribute, DenseInstance, Instance, Instances, SerializationHelper }
import weka.filters.Filter
import weka.filters.unsupervised.attribute.StringToNominal

/**
  * Created by ReggieYang on 2017/4/9.
  */
class MainWorkFlow(conn: Connection) {

  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val pvPath: String = "data/word2vec/summary.pv"
  lazy val vec: ParagraphVectors = WordVectorSerializer.readParagraphVectors(pvPath)

  def run(): Unit = {
    //Summary has been saved in db
    val se = new SummaryExtraction(conn)
    //    val summaryTableName = "search_res_attacker"

    Array("search_res_cve_crafted_denial").foreach(summaryTableName => {
      se.writeVectors(summaryTableName)
      logger.info(s"Vectors are saved in table: ${summaryTableName}_vector")

      val remark = summaryTableName.split("_").drop(2).mkString("_")
      val mt = new ModelTrain()
      Array("category", "impact", "amount").foreach(feature => {
        getTrainData(feature, summaryTableName + "_vector", remark = remark)
        mt.crossValidate2(feature, remark = remark)
      })
    })
  }

  def eval(product: String): Unit = {
    val t = new DefaultTokenizerFactory()
    t.setTokenPreProcessor(new CommonPreprocessor())
    vec.setTokenizerFactory(t)
    //
    //    val hc = new HtmlCrawler
    //    hc.init()
    //    logger.info(s"processing product: $product")
    //
    //    val summary = Array("cve", "cross", "crafted`").flatMap(fw => {
    //      val kw = s"$product+$fw"
    //      val bingRes: Array[String] = hc.getBingRes(kw)
    //      logger.info("sites:" + bingRes.mkString(","))
    //      bingRes.map(url => hc.crawlPage(url))
    //    }).mkString("\t")

    val summary = scala.io.Source.fromFile("data/vector1").getLines().mkString("\t")
    //
    val pw = new PrintWriter("data/evaluation_report")
    //    pw.write(summary)
    //    pw.close()
    logger.info(s"get summary for $product")

    val vector = vec.inferVector(summary)
    logger.info("vector" + vector)

    Array("amount", "impact", "category").foreach(indicator => {
      val dimension = 100
      val atts = new util.ArrayList[Attribute]()
      Range(0, dimension).foreach(i => {
        val dim = new Attribute(s"v$i")
        atts.add(dim)
      })

      if (indicator != "category") {

        atts.add(new Attribute(indicator))
        val instances = new Instances(indicator, atts, 0)
        instances.setClassIndex(instances.numAttributes() - 1)
        val values = (vector.toString.dropRight(1).drop(1) + ",0").split(",").map(_.toDouble)

        val cls = SerializationHelper.read(s"data/wekaData/model2/${indicator}_model.cls").asInstanceOf[RandomForest]
        val di = new DenseInstance(1d, values)
        instances.add(di)

        val pred = cls.classifyInstance(instances.get(0))
        logger.info(s"$indicator:$pred")
        pw.println(s"$indicator:$pred")

      }

      else {
        val alist = new util.ArrayList[String]()
        "CWE-20,CWE-94,CWE-399,CWE-264,CWE-119,CWE-200,CWE-255".split(",").foreach(category => {
          alist.add(category)
        })

        atts.add(new Attribute(indicator, alist))
        val instances = new Instances(indicator, atts, 0)
        val dimensions = vector.toString.drop(1).dropRight(1).split(",")
        val values = new Array[Double](dimension + 1)
        val category = "CWE-20"
        Range(0, dimension).foreach(i => {
          values(i) = dimensions(i).toDouble
        })
        values(dimension) = instances.attribute(dimension).addStringValue(category).toDouble
        val di = new DenseInstance(1d, values)
        instances.add(di)
        instances.setClassIndex(instances.numAttributes() - 1)

        val filter = new StringToNominal
        filter.setInputFormat(instances)
        val ins = Filter.useFilter(instances, filter)

        val cls = SerializationHelper.read(s"data/wekaData/model2/${indicator}_model.cls").asInstanceOf[RandomForest]
        val predictedSeq = cls.distributionForInstance(ins.get(0)).zipWithIndex.sortBy(0 - _._1).map(_._2)
          .map(i => alist.get(i)).mkString(",")

        logger.info(s"$indicator:$predictedSeq")
        pw.println(s"$indicator:$predictedSeq")
      }
    })

    pw.close()

  }

  def getTrainData(feature: String, vectorTableName: String, remark: String = ""): Unit = {
    val sql = s"select CONCAT_WS(',', srv.vector, v.$feature) as output " +
      s"FROM $vectorTableName srv, vul_$feature v WHERE srv.product = v.`name`"
    logger.info(sql)
    val stmt = conn.createStatement()
    conn.setAutoCommit(false)
    val rs = stmt.executeQuery(sql)
    val dimension = 100
    val atts = new util.ArrayList[Attribute]()
    Range(0, dimension).foreach(i => {
      val dim = new Attribute(s"v$i")
      atts.add(dim)
    })

    val outputPath = s"data/wekaData/train2/${feature}_$remark.arff"

    if (feature != "category") {
      logger.info(s"Processing $feature")
      atts.add(new Attribute(feature))
      val instances = new Instances(feature, atts, 0)
      while (rs.next()) {
        val values = rs.getString(1).split(",").map(_.toDouble)
        val di = new DenseInstance(1d, values)
        instances.add(di)
      }
      DataSink.write(outputPath, instances)
    } else {
      logger.info(s"Processing $feature")
      atts.add(new Attribute(feature, null: util.ArrayList[String]))
      val instances = new Instances(feature, atts, 0)
      while (rs.next()) {
        val dimensions = rs.getString(1).split(",").take(dimension)
        val category = rs.getString(1).split(",").takeRight(1)(0)
        val values = new Array[Double](dimension + 1)
        Range(0, dimension).foreach(i => {
          values(i) = dimensions(i).toDouble
        })
        values(dimension) = instances.attribute(dimension).addStringValue(category).toDouble
        val di = new DenseInstance(1d, values)
        instances.add(di)
      }
      val filter = new StringToNominal
      filter.setInputFormat(instances)
      DataSink.write(outputPath, Filter.useFilter(instances, filter))
    }

    logger.info(s"Training data is saved at $outputPath")

  }

}
