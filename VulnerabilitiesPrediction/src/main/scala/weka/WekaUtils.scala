package weka

import java.io.{ File, PrintWriter }
import java.sql.Connection

import util.ResultSetIt
import weka.classifiers.Classifier
import weka.core.Instances
import weka.core.converters.ConverterUtils.DataSource

/**
  * Created by ReggieYang on 2017/3/9.
  */
object WekaUtils {

  def testModel(model: Classifier, testFile: String, classIndex: Int) = {
    val testSet = DataSource.read(testFile)
    testSet.setClassIndex(classIndex)
    println("Actual\tPredicted")
    Range(0, testSet.size()).foreach(index => {
      val pred = model.classifyInstance(testSet.get(index))
      val actual = testSet.get(index).classValue()
      println(s"$actual\t$pred")
    })
  }

  def genArff(conn: Connection, output: String = "impact", path: String = "E:\\secdata\\wekaData\\train\\") = {
    val sql = s"SELECT concat_ws(',', vector, $output) from feature_output2 where $output is not null"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    val data =
      output match {
        case "impact" =>
          val rawData = new ResultSetIt(rs).toArray
          val sep = ","
          rawData.map(line => {
            val vectorImpact = line.split(sep)
            val roundedImpact = Math.round(vectorImpact(vectorImpact.length - 1).toDouble)
            vectorImpact.dropRight(1).mkString(sep) + sep + roundedImpact
          })

        case _ => new ResultSetIt(rs).toArray
      }

    val pw = new PrintWriter(new File(s"$path$output.arff"))
    pw.println(s"@relation text_$output")
    Range(1, 101).foreach(i => {
      pw.println(s"@attribute v$i numeric")
    })

    val outputType = output match {
      case "amount" => "numeric"
      case "impact" => "numeric"
      case "category" => "string"
      case _ => "string"
    }

    pw.println(s"@attribute $output $outputType")
    pw.println("@data")
    data.foreach(line => pw.println(line))
    pw.close()

  }

  def calCRE(cls: Classifier, testData: Instances) = {
    val errors = Range(0, testData.size()).map(index => {
      val pred = cls.classifyInstance(testData.get(index))
      val actual = testData.get(index).classValue()
      //      Math.abs(pred - actual) * 100 / (actual + 1)
      (pred, actual)
    })
      .filter(_._2 > 200)
      .map(x => Math.abs(x._1 - x._2) * 100 / (x._2 + 1))
    errors.sum / errors.length
  }

  def calCRE2(cls: Classifier, testData: Instances) = {
    val errors = Range(0, testData.size()).map(index => {
      val pred = cls.classifyInstance(testData.get(index))
      val actual = testData.get(index).classValue()
      //      Math.abs(pred - actual) * 100 / (actual + 1)
      (pred, actual)
    })
      .map(x => Math.abs(x._1 - x._2) * 100 / (x._2 + 1))

    errors.sum / errors.length
  }

  def calFRank(cls: Classifier, testData: Instances) = {
    val fRanks = Range(0, testData.size()).map(index => {
      val predictedSeq = cls.distributionForInstance(testData.get(index)).zipWithIndex.sortBy(0 - _._1).map(_._2)
      predictedSeq.indexOf(testData.get(index).classValue()).toDouble + 1d
    })

    //    val x = fRanks.toArray
    //    println(x.groupBy(y => y).map(z => (z._1, z._2.length.toDouble * 100 / fRanks.length)).toSeq.sortBy(0 - _._2))
    //    println(fRanks.sum)
    //    println(fRanks.length)
    fRanks.sum / fRanks.length
  }

}
