package nvd.data

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.sql.Connection

import nvd.model.NvdItem
import org.apache.commons.io.FileUtils
import org.dom4j.Element
import org.dom4j.io.SAXReader
import util.{ResultSetIt, ResultSetIt2}
import util.Utils._

/**
  * Created by ReggieYang on 2016/10/22.
  */
class RawDataProcess {

  def readData(conn: Connection) = {
    val yearList = Range(2016, 2017)
    val nd = new NvdItemDao(conn)
    yearList.foreach(year => {
      val filePath = "data\\rawData\\nvdcve-2.0-" + year + ".xml"
      val items = getNvdItems(filePath)
      nd.saveFeature(items)
      //      nd.saveItem(items)
    })
  }

  def readProduct(): Array[String] = {
    val yearList = Range(2002, 2018)
    var pSet = Set[String]()

    yearList.foreach(year => {
      val filePath = "data\\rawData\\nvdcve-2.0-" + year + ".xml"
      val temp = getProductList(filePath)
      pSet = pSet ++ temp
    })

    println(pSet.toArray.mkString(","))

    //    val bw = new BufferedWriter(new FileWriter(new File("data\\productList\\productList")))
    //
    //    pSet.foreach(product => {
    //      bw.write(product + "\n")
    //    })
    //
    //    bw.close()
    pSet.toArray
  }

  def concatElement(entry: Element, name: String, sonName: String, attribute: String = ""): String = {
    val node = entry.element(name)
    if (node == null) {
      EmptyString
    }
    else {
      val elements = node.elements(sonName).toArray().map(_.asInstanceOf[Element])
      var res = EmptyString
      elements.foreach(element => {
        val temp = if (!attribute.isEmpty) element.attribute(attribute).getValue else element.getStringValue
        res = res + temp + EmptyString + TabSep
      })
      res
    }
  }


  def getNvdItems(filePath: String): Array[NvdItem] = {
    val reader = new SAXReader()
    val document = reader.read(new File(filePath))
    val node = document.getRootElement
    //初始化dom4j作为xml解析器，读取待分析的文档

    val elements = node.elements().toArray().map(_.asInstanceOf[Element])
    elements.map(entry => {
      val id = entry.element("cve-id").getStringValue
      val products2 = concatElement(entry, "vulnerable-software-list", "product")
      val products = extractProduct(products2)
      //将vulnerable-software-list标签下的product标签所有内容拼接，得到受漏洞影响的软件列表
      //      if (products.length > 20000) println(id + ": " + products.length)
      val impactScore = if (entry.element("cvss") != null) entry.element("cvss").element("base_metrics").element("score").getStringValue.toDouble else 0d
      val cwe = if (entry.element("cwe") != null) entry.element("cwe").attribute("id").getValue else EmptyString
      val reference = concatElement(entry, "references", "reference", "href")
      val summary = entry.element("summary").getStringValue
      val cvdItem = NvdItem(id, products, impactScore, reference, cwe, summary)
      //获取漏洞严重程度，漏洞类型，漏洞描述信息等，封装成对象，准备存储在数据库中
      cvdItem
    })
  }

  def getProductList(filePath: String): Set[String] = {
    val reader = new SAXReader()
    val document = reader.read(new File(filePath))
    val node = document.getRootElement
    var pSet = Set[String]()

    val elements = node.elements().toArray().map(_.asInstanceOf[Element])
    elements.foreach(entry => {
      val products2 = concatElement(entry, "vulnerable-software-list", "product")
      val products = extractProduct(products2)
      val tempSet = products.split(TabSep).toSet
      pSet = pSet ++ tempSet
    })

    pSet
  }

  def extractProduct(products: String): String = {
    val productList = products.split(TabSep)
    productList.map(product => {
      if (!product.isEmpty) {
        val title = product.split(":")
        if (title.length >= 5) title(3) + " " + title(4) else title(title.length - 1)
      }
    }).toSet.mkString(TabSep)
  }

  def writeSummary(rdp: RawDataProcess) = {
    Range(2002, 2018).foreach(year => {
      rdp.getNvdItems(s"data\\rawData\\nvdcve-2.0-$year.xml").foreach(item => {
        FileUtils.write(new File("E:\\secdata\\summaryById\\" + item.id), item.summary)
      })
    })
  }


  def writeVulAmount(conn: Connection) = {
    val sql = s"SELECT * from vul_amount"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    val data = new ResultSetIt2(rs).toArray
    data
  }

  def getSearchSite(conn: Connection) = {
    val sql = s"select distinct res from product_search"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt(rs).toArray
  }

  def getSearchSite2(conn: Connection) = {
    val sql = s"select id, url from search_res2"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt2(rs).toArray
  }

  def writeDesctoFile(conn: Connection) = {
    val sql = s"select sr.res from search_res2 sr"
    val pw = new PrintWriter("data\\desc.txt")
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    while(rs.next()) {
      pw.println(rs.getString(1))
    }
    pw.close()
  }

}
