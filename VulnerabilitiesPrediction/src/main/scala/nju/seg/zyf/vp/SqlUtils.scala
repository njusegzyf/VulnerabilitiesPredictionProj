package nju.seg.zyf.vp

import scala.collection.mutable

import java.sql.{ Connection, DriverManager, ResultSet, Statement }
import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Strings

import nju.seg.zyf.vp.app.ExpConfigs

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object SqlUtils {

  final case class DbConnectionConfig(connectionString: String,
                                      user: String,
                                      password: String)

  @Nonnegative
  val minimumSummarySize: Int = ExpConfigs.defaultExpMinimumSummarySize

  /**
    * @see [[nvd.data.DBConnection]]
    */
  def useDbConnection[T](config: DbConnectionConfig, func: Connection => T): T = {
    Class.forName("com.mysql.jdbc.Driver")
    val DbConnectionConfig(connectionString, user, password) = config
    val conn = DriverManager.getConnection(connectionString, user, password)

    try {
      func(conn)
    } finally {
      conn.close()
    }
  }

  /** Gets product summary and category pairs from database. */
  def getProductSummaryAndCategory(productSummaryTable: String,
                                   categoryTable: String,
                                   sizeLimit: Int = 10000,
                                   productFiledInProductSummaryTable: String = "product",
                                   productSummaryFieldInProductSummaryTable: String = "summary",
                                   productFiledInCategoryTable: String = "name",
                                   categoryFiledInCategoryTable: String = "category")
                                  (implicit dbConnectionConfig: DbConnectionConfig)
  : Vector[(String, String)] = {
    require(!Strings.isNullOrEmpty(productSummaryTable))
    require(!Strings.isNullOrEmpty(categoryTable))
    require(dbConnectionConfig != null)

    val sqlCommand =
      s"""SELECT ps.$productSummaryFieldInProductSummaryTable AS summary, vc.$categoryFiledInCategoryTable AS category
         |FROM $productSummaryTable AS ps, $categoryTable AS vc
         |WHERE ps.$productSummaryFieldInProductSummaryTable is not null
         |  AND LENGTH(ps.$productSummaryFieldInProductSummaryTable) > ${ this.minimumSummarySize }
         |  AND ps.$productFiledInProductSummaryTable = vc.$productFiledInCategoryTable"""
      .stripMargin

    SqlUtils.executeSqlCommandAndBuildResultVector(sqlCommand,
                                                   dbConnectionConfig,
                                                   rs => Utils.removeEndLineChars(rs.getString("summary")),
                                                   rs => Utils.removeEndLineChars(rs.getString("category")),
                                                   sizeLimit)
  }

  /** Gets product summary and amount pairs from database. */
  def getProductSummaryAndAmount(productSummaryTable: String,
                                 amountTable: String,
                                 sizeLimit: Int = 10000,
                                 amountFilter: Option[Double => Boolean] = Option.empty,
                                 productFiledInProductSummaryTable: String = "product",
                                 productSummaryFieldInProductSummaryTable: String = "summary",
                                 productFiledInCategoryTable: String = "name",
                                 amountFiledInCategoryTable: String = "amount")
                                (implicit dbConnectionConfig: DbConnectionConfig)
  : Vector[(String, Double)] = {
    require(!Strings.isNullOrEmpty(productSummaryTable))
    require(!Strings.isNullOrEmpty(amountTable))
    require(dbConnectionConfig != null)

    val sqlCommand =
      s"""SELECT ps.$productSummaryFieldInProductSummaryTable AS summary, va.$amountFiledInCategoryTable AS amount
         |FROM $productSummaryTable AS ps, $amountTable AS va
         |WHERE ps.$productSummaryFieldInProductSummaryTable is not null
         |  AND LENGTH(ps.$productSummaryFieldInProductSummaryTable) > ${ this.minimumSummarySize }
         |  AND ps.$productFiledInProductSummaryTable = va.$productFiledInCategoryTable"""
      .stripMargin

    val actualAmountFilter: (Double) => Boolean = amountFilter.getOrElse(ExpConfigs.amountFilter)

    SqlUtils.executeSqlCommandAndBuildResultVector(sqlCommand,
                                                   dbConnectionConfig,
                                                   rs => Utils.removeEndLineChars(rs.getString("summary")),
                                                   _.getDouble("amount"),
                                                   sizeLimit,
                                                   (_: String, amount: Double) => actualAmountFilter(amount))
  }

  /** Gets product summary and impact pairs from database. */
  def getProductSummaryAndImpact(productSummaryTable: String,
                                 impactTable: String,
                                 sizeLimit: Int = 10000,
                                 productFiledInProductSummaryTable: String = "product",
                                 productSummaryFieldInProductSummaryTable: String = "summary",
                                 productFiledInCategoryTable: String = "name",
                                 impactFiledInCategoryTable: String = "impact")
                                (implicit dbConnectionConfig: DbConnectionConfig)
  : Vector[(String, Double)] = {
    require(!Strings.isNullOrEmpty(productSummaryTable))
    require(!Strings.isNullOrEmpty(impactTable))
    require(dbConnectionConfig != null)

    val sqlCommand: String =
      s"""SELECT ps.$productSummaryFieldInProductSummaryTable AS summary, vi.$impactFiledInCategoryTable AS impact
         |FROM $productSummaryTable AS ps, $impactTable AS vi
         |WHERE ps.$productSummaryFieldInProductSummaryTable is not null
         |  AND LENGTH(ps.$productSummaryFieldInProductSummaryTable) > ${ this.minimumSummarySize }
         |  AND ps.$productFiledInProductSummaryTable = vi.$productFiledInCategoryTable"""
      .stripMargin

    SqlUtils.executeSqlCommandAndBuildResultVector(sqlCommand,
                                                   dbConnectionConfig,
                                                   rs => Utils.removeEndLineChars(rs.getString("summary")),
                                                   _.getDouble("impact"),
                                                   sizeLimit)
  }

  def executeSqlCommandAndBuildResultVector[TData1, TData2](sqlCommand: String,
                                                            dbConnectionConfig: DbConnectionConfig,
                                                            getData1Func: ResultSet => TData1,
                                                            getData2Func: ResultSet => TData2,
                                                            sizeLimit: Int = 10000,
                                                            dataFilter: (TData1, TData2) => Boolean = { (_: TData1, _: TData2) => true })
  : Vector[(TData1, TData2)] = {
    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()
      val rs: ResultSet = stmt.executeQuery(sqlCommand)

      val resultBuilder: mutable.Builder[(TData1, TData2), Vector[(TData1, TData2)]] = Vector.newBuilder[(TData1, TData2)]
      var index: Int = 0
      while (rs.next() && index < sizeLimit) {
        val data1 = getData1Func(rs)
        val data2 = getData2Func(rs)
        if (dataFilter(data1, data2)) {
          resultBuilder += Tuple2(data1, data2)
          index += 1
        }
      }
      resultBuilder.result()
    })
  }

  @Nonnull @CheckReturnValue
  def getValidProductNamesWithVersion(minNameAndVersionLen: Int = 8)
                                     (implicit dbConnectionConfig: DbConnectionConfig)

  : Vector[String] = {
    require(dbConnectionConfig != null)
    require(minNameAndVersionLen >= 0)

    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()

      def getAllValidNamesFromTable(table: String): Vector[String] = {
        val sqlCommand =
          if (table == "vul_amount") {
            s"""SELECT name
               |FROM $table
               |WHERE LENGTH(name) > $minNameAndVersionLen AND amount >= ${ ExpConfigs.defaultExpMinimumAmount }""".stripMargin
          } else {
            s"""SELECT name
               |FROM $table
               |WHERE LENGTH(name) > $minNameAndVersionLen""".stripMargin
          }
        val rs: ResultSet = stmt.executeQuery(sqlCommand)

        val resultBuilder: mutable.Builder[String, Vector[String]] = Vector.newBuilder[String]
        while (rs.next()) {
          resultBuilder += rs.getString(1)
        }
        resultBuilder.result()
      }

      val tables: Seq[String] = Seq("vul_category", "vul_amount", "vul_impact")
      assert(tables.nonEmpty)
      val nameVectors: Seq[Vector[String]] = tables map { table => getAllValidNamesFromTable(table) }
      val jointNames: Vector[String] = nameVectors.reduce { (v1, v2) => v1.intersect(v2) }

      jointNames
    })
  }

  @Nonnull @CheckReturnValue
  def getProductToCategoryMap(implicit dbConnectionConfig: DbConnectionConfig): mutable.HashMap[String, String] = {
    require(dbConnectionConfig != null)

    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()
      val sqlCommand = raw"SELECT * FROM vul_category"
      val rs: ResultSet = stmt.executeQuery(sqlCommand)

      val result: mutable.HashMap[String, String] = mutable.HashMap[String, String]()
      while (rs.next()) {
        result += rs.getString(1) -> rs.getString(2)
      }
      result
    })
  }

  @Nonnull @CheckReturnValue
  def getProductToAmountMap(implicit dbConnectionConfig: DbConnectionConfig): mutable.HashMap[String, String] = {
    require(dbConnectionConfig != null)

    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()
      val sqlCommand = raw"SELECT * FROM vul_amount"
      val rs: ResultSet = stmt.executeQuery(sqlCommand)

      val result: mutable.HashMap[String, String] = mutable.HashMap[String, String]()
      while (rs.next()) {
        result += rs.getString(1) -> rs.getString(2)
      }
      result
    })
  }

  @Nonnull @CheckReturnValue
  def getProductToImpactMap(implicit dbConnectionConfig: DbConnectionConfig): mutable.HashMap[String, String] = {
    require(dbConnectionConfig != null)

    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()
      val sqlCommand = raw"SELECT * FROM vul_impact"
      val rs: ResultSet = stmt.executeQuery(sqlCommand)

      val result: mutable.HashMap[String, String] = mutable.HashMap[String, String]()
      while (rs.next()) {
        result += rs.getString(1) -> rs.getString(2)
      }
      result
    })
  }

  /** For tests only. */
  @Deprecated
  @Nonnull @CheckReturnValue
  def getDummySummaries(implicit dbConnectionConfig: DbConnectionConfig): Vector[String] = {
    require(dbConnectionConfig != null)

    SqlUtils.useDbConnection(dbConnectionConfig, { connection =>
      val stmt: Statement = connection.createStatement()
      val sqlCommand = raw"SELECT res FROM search_res2 WHERE LENGTH(res) > $minimumSummarySize"
      val rs: ResultSet = stmt.executeQuery(sqlCommand)

      val result: mutable.Builder[String, Vector[String]] = Vector.newBuilder[String]
      while (rs.next()) {
        result += rs.getString(1)
      }
      result.result()
    })
  }
}
