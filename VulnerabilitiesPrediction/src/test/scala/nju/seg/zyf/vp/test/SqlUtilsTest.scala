package nju.seg.zyf.vp.test

import java.sql.{ Connection, Statement }

import org.assertj.core.api.Assertions
import org.testng.Assert
import org.testng.annotations.Test

import nju.seg.zyf.vp.SqlUtils
import nju.seg.zyf.vp.app.ExpConfigs

/** Tests for [[nju.seg.zyf.vp.SqlUtils]].
  *
  * @author Zhang Yifan
  */
final class SqlUtilsTest {

  @Test
  def useDbConnection_OkForLocal(): Unit = {
    // Given(SetUp)
    val connectionConfig: SqlUtils.DbConnectionConfig = ExpConfigs.mySQLConnectionConfig
    val dummyOutput = -1

    // When(Exercise)
    val output = SqlUtils.useDbConnection[Int](connectionConfig, (connection: Connection) => {
      //noinspection ScalaUnusedSymbol
      val stmt: Statement = connection.createStatement()
      dummyOutput
    })

    // Then(Verify)
    Assert.assertEquals(output, dummyOutput)
  }


  @Test
  def getValidProductNamesWithVersion_OkForLocal(): Unit = {
    // Given(SetUp)
    implicit val connectionConfig: SqlUtils.DbConnectionConfig = ExpConfigs.mySQLConnectionConfig

    // When(Exercise)
    val output = SqlUtils.getValidProductNamesWithVersion(30)

    // Then(Verify)
    Assert.assertTrue(output.nonEmpty)
  }


  @org.testng.annotations.BeforeClass
  def setUp(): Unit = {}

  @org.testng.annotations.AfterClass
  def tearDown(): Unit = {}
}
