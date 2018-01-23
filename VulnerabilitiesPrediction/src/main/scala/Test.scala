import java.io._
import java.sql.Connection
import java.util
import java.util.Random
import java.util.logging.Level

import com.gargoylesoftware.htmlunit.{IncorrectnessListener, WebClient}
import com.gargoylesoftware.htmlunit.html._
import crawler.HtmlCrawler
import lucene.{FeatureExtraction, LuceneUtils}
import nvd.data.{DBConnection, NvdItemDao, RawDataProcess, SummaryExtraction}
import nvd.model.{ProductSearch, SearchRes, SearchRes2}
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.LogFactory
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.deeplearning4j.bagofwords.vectorizer.BagOfWordsVectorizer
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.sentenceiterator.{LineSentenceIterator, SentenceIterator}
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.{DefaultTokenizerFactory, TokenizerFactory}
import org.deeplearning4j.util.ModelSerializer
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import rnn.{RnnData, RnnUtils, RnnWorkFlow}

import collection.JavaConverters._
import w2v.{W2VRNN, Word2VecUtils}
import weka.classifiers.`lazy`.IBk
import weka.classifiers.bayes.{NaiveBayes, NaiveBayesMultinomial}
import weka.{ModelTrain, WekaUtils}
import weka.classifiers.{Classifier, Evaluation}
import weka.classifiers.functions._
import weka.classifiers.meta.{AdaBoostM1, LogitBoost, MultiClassClassifier, Stacking}
import weka.classifiers.rules.{JRip, OneR, PART}
import weka.classifiers.trees._
import weka.core.{Attribute, Instances, SerializationHelper}
import weka.core.converters.ConverterUtils.DataSource
import weka.filters.unsupervised.attribute.StringToNominal
import workflow.MainWorkFlow


/**
  * Created by ReggieYang on 2016/10/22.
  */
object Test {

  System.setProperty("hadoop.home.dir", "/Users/kaimaoyang/Applications/winutil")
  System.setProperty("SPARK_LOCAL_IP", "127.0.0.1")
  System.setProperty("SPARK_MASTER_IP", "127.0.0.1")


  lazy val conf = new SparkConf().
    setMaster("local[10]").
    setAppName("My App").
    set("spark.cores.max", "10")
  lazy val sc = new SparkContext(conf)

  lazy val logger = LoggerFactory.getLogger(Test.getClass)

  lazy val featureWords = Array("attacker", "arbitrary", "vulnerability", "cve", "unspecified", "denial", "crafted", "cross")

  def main(args: Array[String]) = {
    val conn = DBConnection.getConnection
    //    val mt = new ModelTrain
    //    Array("impact", "category", "amount").foreach(indicator => {
    //      mt.trainModel(s"data/wekaData/train2/${indicator}_cve_crafted_vulnerability.arff", new RandomForest,
    //        s"data/wekaData/model2/${indicator}_model.cls", 100)
    //    })


    //    W2VRNN.t2v("data/word2vec/summary.pv", "/Users/kaimaoyang/Downloads/dl4j-data2")

    val rwf = new RnnWorkFlow("data/word2vec/summary.pv")
    rwf.trainModel()


    //        W2VRNN.t2v("/Users/kaimaoyang/Documents/machine-learning resources/glove.6B/glove.6B.50d.txt"
    //          , "/Users/kaimaoyang/Downloads/dl4j-data2")
    //    val mw = new MainWorkFlow(conn)
    //    mw.eval("acrobat+9.1")
    conn.close()

  }


  def search() = {

    val productRDD = sc.textFile("data/vulAmount/part-00000")
    //    val productRDD = scala.io.Source.fromFile("data/vulAmount/product").getLines().toArray

    productRDD.sortBy(x => x).foreachPartition(it => {
      val conn = DBConnection.getConnection
      val hc = new HtmlCrawler
      val nd = new NvdItemDao(conn)
      hc.init()
      var i = 0
      var tempPs = Array[ProductSearch]()
      it.filter(p => p.split("\t")(1).toInt > 1).foreach(productWithAmount => {
        val product = productWithAmount.split("\t")(0)

        i = i + 1
        println(s"processing product: $product")

        val res = Array("cve", "cross").flatMap(fw => {
          val kw = s"$product+$fw"
          val bingRes = hc.getYahooRes(kw)
          println("product: " + kw + " bingRes: " + bingRes.mkString(","))
          bingRes.map(search => ProductSearch(kw, Array(search)))
        })

        //          hc.getBingRes(product).map(search => ProductSearch(product, Array(search)))

        //        val ps = ProductSearch(product, hc.crawlPage(hc.getBingRes(product)))
        //        tempPs = tempPs :+ ps
        tempPs = tempPs ++ res

        //        if (i % 1 == 0) {
        nd.saveSearchRes(res, "yahoo")
        //          tempPs = Array[ProductSearch]()
        //        }
      })
      //      nd.saveSearchRes(res, "baidu")

      conn.close()
    })
  }


  def search3(engine: String) = {
    val productRDD = scala.io.Source.fromFile("data/vulAmount/product").getLines().toArray.drop(33)

    val conn = DBConnection.getConnection
    val hc = new HtmlCrawler
    val nd = new NvdItemDao(conn)
    hc.init()
    productRDD.sortBy(x => x).foreach(product => {
      println(s"processing product: $product")

      val res = Array("cve").flatMap(fw => {
        val kw = s"$product+$fw"

        val bingRes: Array[String] = engine match {
          case "google" => hc.getGoogleRes(kw)
          case "yahoo" => hc.getYahooRes(kw)
          case "baidu" => hc.getBaiduRes(kw)
        }

        logger.info("sites:" + bingRes.mkString(","))

        val searchRes = bingRes.map(url => hc.crawlPage(url))

        //        println("product: " + kw + " bingRes: " + bingRes.mkString(","))
        searchRes.map(search => ProductSearch(kw, Array(search)))
      })

      nd.saveSearchRes(res, engine)
    })
    conn.close()

  }

  def search2(data: Array[Array[String]]) = {
    val siteRDD = sc.parallelize(data)

    siteRDD.foreachPartition(it => {
      val conn = DBConnection.getConnection
      val hc = new HtmlCrawler
      val nd = new NvdItemDao(conn)
      hc.init()
      var i = 0
      //      var tempPs = Array[SearchRes]()
      it.foreach(siteWithId => {
        val id = siteWithId(0)
        val site = siteWithId(1)
        i = i + 1
        println(s"processing site: $site")

        val res = hc.crawlPage(site)
        val sr = SearchRes2(id.toInt, site, res)
        nd.saveSearchSiteRes(sr)
        //        tempPs = tempPs :+ sr
        //
        //        if (i % 2 == 0) {
        //          nd.saveSearchSiteRes(tempPs)
        //          tempPs = Array[SearchRes]()
        //        }
      })
      //      nd.saveSearchSiteRes(tempPs)
      conn.close()
    })
  }

  def crossValidate() = {
    //        val folds = 10
    //        val rand = new Random(1)
    //        val randData = new Instances(trainData)
    //        randData.randomize(rand)
    //        randData.stratify(folds)
    //        Range(0, folds).foreach(i => {
    //          val train = randData.trainCV(folds, i)
    //          train.setClassIndex(classIndex)
    //          val test = randData.testCV(folds, i)
    //          trainData.setClassIndex(classIndex)
    //          val eval = new Evaluation(train)
    //          cls.buildClassifier(train)
    //          eval.evaluateModel(cls, test)
    //          SerializationHelper.write(s"E:\\secdata\\wekaData\\evaluation\\eval_$i", eval)
    //          logger.info(eval.toSummaryString())
    //        })
  }


}
