package util

import java.io.{BufferedWriter, File, FileWriter}
import java.sql.ResultSet

/**
  * Created by ReggieYang on 2016/10/22.
  */
object Utils {

  lazy val TabSep = "\t"
  lazy val EmptyString = ""

  def writeFile(filePath: String, content: String) = {
    val file = new File(filePath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()
  }
}

class ResultSetIt(rs: ResultSet) extends Iterator[String] {
  override def hasNext: Boolean = rs.next()

  override def next(): String = rs.getString(1)
}

class ResultSetIt2(rs: ResultSet) extends Iterator[Array[String]] {
  override def hasNext: Boolean = rs.next()

  override def next(): Array[String] = Range(1, rs.getMetaData.getColumnCount + 1).map(i => rs.getString(i)).toArray
}

