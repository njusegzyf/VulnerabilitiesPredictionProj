package nju.seg.zyf.vp.test

import java.io.File

import com.google.common.collect.ImmutableList
import com.google.common.io.Files

import org.apache.commons.io.Charsets
import org.testng.Assert
import org.testng.annotations.Test

import nju.seg.zyf.vp.RnnUtils

/** Tests for [[nju.seg.zyf.vp.RnnUtils]].
  *
  * @author Zhang Yifan
  */
@Test
final class RnnUtilsTest {

  @Test
  def parseRnnLogSimple_OkOnSample1(): Unit = {
    // Given(SetUp)
    val logContent: String = LogSamples.logSample1

    val logLines : Vector[String] = logContent.lines.toVector

    // When(Exercise)
    val parseResult = RnnUtils.parseRnnLogSimple(logLines, 2, 128, java.lang.Double.parseDouble)

    // Then(Verify)
    Assert.assertEquals(parseResult.rnnLayerNum, 2)
    Assert.assertEquals(parseResult.rnnUnitNum, 128)
    Assert.assertEquals(parseResult.stateDimension, parseResult.rnnUnitNum * 4)
    Assert.assertEquals(parseResult.size, 2)
  }

  @Test
  def parseRnnLog_OkOnSample1(): Unit = {
    // Given(SetUp)
    val logContent: String = LogSamples.logSample1

    val logLines : Vector[String] = logContent.lines.toVector

    // When(Exercise)
    val parseResult = RnnUtils.parseRnnLog(logLines, 2, 128)

    // Then(Verify)
    Assert.assertEquals(parseResult.rnnLayerNum, 2)
    Assert.assertEquals(parseResult.rnnUnitNum, 128)
    Assert.assertEquals(parseResult.stateDimension, parseResult.rnnUnitNum * 4)
    Assert.assertEquals(parseResult.size, 2)
  }

  @Test
  def parseRnnLog_OkOnSample2(): Unit = {
    // Given(SetUp)
    val logContent: String = LogSamples.logSample2

    val logLines : Vector[String] = logContent.lines.toVector

    // When(Exercise)
    val parseResult = RnnUtils.parseRnnLog(logLines, 2, 128)

    // Then(Verify)
    Assert.assertEquals(parseResult.rnnLayerNum, 2)
    Assert.assertEquals(parseResult.rnnUnitNum, 128)
    Assert.assertEquals(parseResult.stateDimension, parseResult.rnnUnitNum * 4)
    Assert.assertEquals(parseResult.size, 4)
  }

  @Test(enabled = false)
  def parseRnnLog_OkOnBigData(): Unit = {
    // Given(SetUp)
    val logLines : ImmutableList[String] = Files.asCharSource(new File("E:/Exp1_Category_Data_Summary_Infer_Err_UTF8.txt"), Charsets.UTF_8).readLines()

    // When(Exercise)
    import scala.collection.JavaConverters.asScalaBuffer
    val parseResult = RnnUtils.parseRnnLog(asScalaBuffer(logLines).toVector, 2, 128)

    // Then(Verify)
    Assert.assertEquals(parseResult.rnnLayerNum, 2)
    Assert.assertEquals(parseResult.rnnUnitNum, 128)
    Assert.assertEquals(parseResult.stateDimension, parseResult.rnnUnitNum * 4)
    // Assert.assertEquals(parseResult.size, 4)
  }

  @org.testng.annotations.BeforeClass
  def setUp(): Unit = {}

  @org.testng.annotations.AfterClass
  def tearDown(): Unit = {}
}
