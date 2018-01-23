package nvd.data

import java.io.File
import java.sql.Connection

import crawler.HtmlCrawler
import org.apache.commons.io.FileUtils
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory
import org.slf4j.LoggerFactory
import util.Utils._

/**
  * Created by ReggieYang on 2016/10/23.
  */
class SummaryExtraction(conn: Connection) {

  lazy val logger = LoggerFactory.getLogger(this.getClass)


  def featureByCwe() = {
    val sql = "select cwe, GROUP_CONCAT(summary Separator'\\t') as summary from feature f, vulnerability v " +
      "where f.summary not like \"\\*\\* REJECT \\*\\*%\" and f.id = v.id group by v.cwe"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    while (rs.next()) {
      val pathPrefix = "data\\featureByCwe\\"
      val cwe = if (rs.getString("cwe").isEmpty) "cwe-other" else rs.getString("cwe")
      val content = rs.getString("summary")
      writeFile(pathPrefix + cwe, content)
    }
  }

  def writeSummaryByProduct() = {
    val sql = "select pv.product, GROUP_CONCAT(f.summary separator ' ') as summary " +
      "from product_version pv, feature f where f.id = pv.vul GROUP BY pv.product"
    //    val sql2 = "select pv.product, GROUP_CONCAT(f.summary separator ' ') as summary " +
    //      "from product_version pv, feature f where f.id = pv.vul and pv.product like 'freebsd%' GROUP BY pv.product"
    val sql3 = "select * from product_summary"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql3)
    var i = 0
    while (rs.next()) {
      val product = rs.getString("product")
      val summary = rs.getString("summary")
      i = i + 1
      if (i % 100 == 0) println(s"index: $i, product: $product")
      if (!product.isEmpty) {
        FileUtils.write(new File(s"E:\\secdata\\summaryByProduct\\$product"), summary)
      }
    }
  }

  def writeVectors() = {
    val pvPath: String = "D:\\workspace\\VulnerabilitiesPrediction\\data\\word2vec\\summary.pv"
    val t = new DefaultTokenizerFactory()
    t.setTokenPreProcessor(new CommonPreprocessor())
    val vec = WordVectorSerializer.readParagraphVectors(pvPath)
    vec.setTokenizerFactory(t)

    val sql3 = "select * from product_summary"
    val stmt = conn.createStatement()
    conn.setAutoCommit(false)
    val cmd = conn.prepareStatement("insert into product_vector(product, vector) values(?,?)")
    val rs = stmt.executeQuery(sql3)
    var i = 0
    while (rs.next()) {
      val product = rs.getString("product")
      val summary = rs.getString("summary")
      i = i + 1
      if (i % 100 == 0) println(s"index: $i, product: $product")
      cmd.setString(1, product)
      cmd.setString(2, vec.inferVector(summary).toString)
      cmd.addBatch()

      if (i % 1000 == 0) {
        cmd.executeBatch()
        conn.commit()
      }
    }

    cmd.executeBatch()
    conn.commit()
    cmd.close()

  }

  def writeVectors(summaryTableName: String) = {
    val pvPath: String = "data/word2vec/summary.pv"
    val t = new DefaultTokenizerFactory()
    t.setTokenPreProcessor(new CommonPreprocessor())
    val vec = WordVectorSerializer.readParagraphVectors(pvPath)
    vec.setTokenizerFactory(t)
    val createTable = s"CREATE TABLE IF NOT EXISTS `${summaryTableName}_vector` (`id` int(11) NOT NULL AUTO_INCREMENT," +
      "`product` varchar(255) DEFAULT NULL, `vector` mediumtext,  PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8"
    val stmt2 = conn.createStatement()
    stmt2.executeUpdate(createTable)

    logger.info("Table created")
    val sql3 = s"select id, product, res from $summaryTableName"
    val stmt = conn.createStatement()
    conn.setAutoCommit(false)
    val cmd = conn.prepareStatement(s"insert into ${summaryTableName}_vector(product, vector) values(?,?)")
    val rs = stmt.executeQuery(sql3)
    logger.info("Begin to infer vectors")
    while (rs.next()) {
      val product = rs.getString("product")
      val summary = rs.getString("res")
      val id = rs.getString("id")
      logger.info(s"index: $id, product: $product")
      try {
        if (summary != null && summary.length > 0) {
          val vector = vec.inferVector(summary).toString.drop(1).dropRight(1)
          logger.info(s"vector: $vector")
          cmd.setString(1, product)
          cmd.setString(2, vector)
          cmd.addBatch()
          cmd.executeBatch()
          conn.commit()
        }
      }
      catch {
        case e: Throwable => e.printStackTrace()
      }
    }
    cmd.close()
  }

  def featureByImpactScore() = {
    val begin = "SELECT GROUP_CONCAT(summary Separator'\\t') as summary from feature f, vulnerability v " +
      "where f.id = v.id and v.impact_score >= "
    val middle = " and v.impact_score < "

    val scoreRange = Range(1, 11)
    scoreRange.foreach(score => {
      val sql = begin + score + middle + (score + 1)
      println(sql)
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery(sql)
      while (rs.next()) {
        val pathPrefix = "data\\featureByImpactScore\\"
        val fileName = "Impact Score-" + score
        val content = rs.getString("summary")
        writeFile(pathPrefix + fileName, content)
      }
    })


  }

  def featureByProductDB(products: Array[String]) = {
    conn.setAutoCommit(false)
    //    val sql = "select 'git' as product, GROUP_CONCAT(v.summary SEPARATOR '\\t') as summary " +
    //      "from vulnerability_copy v where v.product like \"git\\t%\" or v.product like \"%\\tgit\" or v.product like \"%\\tgit\\t%\" or v.product like \"%git%\""

    //    val sql = "select GROUP_CONCAT(summary SEPARATOR '\\t') as summary from feature f where f.id in " +
    //      "(SELECT v.id FROM vulnerability v WHERE v.product LIKE \"git\\t%\" or v.product like \"%\\tgit\" or v.product like \"%\\tgit\\t%\" or v.product like \"%git%\")"

    val sql = "SELECT 'git' as name, v.id as id FROM vulnerability v " +
      "WHERE v.product LIKE \"git\\t%\" or v.product like \"%\\tgit\" or v.product like \"%\\tgit\\t%\" or v.product like \"git\""

    val cmd = conn.prepareStatement("insert into product(name, vul) values(?,?)")

    val matchText = "git"
    var i = 0

    products.foreach(product => {
      println(i)

      val sqlNew = sql.replaceAll(matchText, product)
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery(sqlNew)
      while (rs.next()) {
        cmd.setString(1, rs.getString("name"))
        cmd.setString(2, rs.getString("id"))
        cmd.addBatch()
        //        val pathPrefix = "data\\featureByProduct\\"
        //        val fileName = product
        //        val content = rs.getString("summary")
        //        writeFile(pathPrefix + fileName, content)
      }
      if ((i % 1000 == 0) || (i >= 30000)) {
        conn.commit()
        cmd.executeBatch()
      }

      i = i + 1
    })

    conn.commit()
    cmd.executeBatch()
    cmd.close()

  }

  def featureByProduct() = {
    val sql = "select p.`name` as name, GROUP_CONCAT(summary SEPARATOR '\\t') as summary from feature f, product p where f.id = p.vul group by p.`name`"
    val cmd = conn.prepareStatement("insert into product(name, vul) values(?,?)")
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    var i = 0

    while (rs.next()) {
      println(i)
      i = i + 1
      val pathPrefix = "data\\featureByProduct\\"
      val name = rs.getString("name")
      val content = rs.getString("summary")
      writeFile(pathPrefix + name, content)
    }

  }


  def crawlReferenceData() = {
    val htmlCrawler = new HtmlCrawler
    htmlCrawler.init()
    conn.setAutoCommit(false)
    val sql = "select id, reference from feature f"
    val cmd = conn.prepareStatement("update feature set reference = ? where id = ?")
    val rs = conn.createStatement().executeQuery(sql)
    var i = 0
    while (rs.next()) {
      i = i + 1
      if (i % 10 == 0) {
        println("index: " + i)
      }
      val summary = htmlCrawler.crawlDescription(rs.getString("reference"))
      cmd.setString(2, rs.getString("id"))
      cmd.setString(1, summary)
      cmd.addBatch()

      if (i % 10 == 0) {
        cmd.executeBatch()
        conn.commit()
        htmlCrawler.close()
        htmlCrawler.init()
      }

    }

    cmd.executeBatch()
    conn.commit()
    cmd.close()

    htmlCrawler.close()
  }


}
