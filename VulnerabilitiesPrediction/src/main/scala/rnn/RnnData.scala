package rnn

import java.sql.Connection

import util.{ResultSetIt, ResultSetIt2}

/**
  * Created by kaimaoyang on 2017/5/11.
  */
object RnnData {

  def getImpact(conn: Connection): Array[Int] = {
    val sql = s"select impact from vul_impact limit 5"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt(rs).toArray.map(impact => {
      Math.round(impact.toDouble).toInt
    })
  }

  def getSummary(conn: Connection): Array[String] = {
    val sql = s"select summary from product_summary limit 5"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt(rs).toArray
  }

  def getSummaryCat(conn: Connection) = {
    val sql = "select ps.summary, vc.category FROM vul_category vc, product_summary ps where vc.name = ps.product"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt2(rs).toArray
  }

  def getSum4Cat(conn: Connection): Array[String] = {
    getSummaryCat(conn).map(a => a(0))
  }

  def getCat(conn: Connection): Array[Int] = {
    getSummaryCat(conn).map(a => {
      RnnUtils.getCatIndex(a(1))
    })
  }

  def getOutputVector(conn: Connection, output: String) = {
    val sql = s"select pv.vector, v.$output " +
      s"from product_vector pv, vul_$output v where v.name = pv.product"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    new ResultSetIt2(rs).toArray
  }

}
