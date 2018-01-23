package rnn

import java.io.PrintWriter
import java.sql.Connection
import java.util.Random

/**
  * Created by kaimaoyang on 2017/5/11.
  */
object RnnUtils {

  lazy val categoryList = Array("CWE-310", "CWE-89", "CWE-200", "CWE-79",
    "CWE-119", "CWE-20", "CWE-94", "CWE-264", "CWE-399", "CWE-284", "CWE-352",
    "CWE-22", "CWE-19", "CWE-287", "CWE-189", "CWE-254", "CWE-16", "CWE-362",
    "CWE-134", "CWE-59", "CWE-255", "CWE-74", "CWE-78", "CWE-502", "CWE-428",
    "CWE-17", "CWE-345", "CWE-77", "CWE-611", "CWE-798", "CWE-326", "CWE-285",
    "CWE-191", "CWE-400", "CWE-426", "CWE-275", "CWE-416", "CWE-190", "CWE-93",
    "CWE-346", "CWE-787", "CWE-18", "CWE-125", "CWE-361", "CWE-601", "CWE-434",
    "CWE-415", "CWE-332", "CWE-918", "CWE-532", "CWE-320", "CWE-476", "CWE-113",
    "CWE-90", "CWE-21", "CWE-640", "CWE-485", "CWE-295", "CWE-693", "CWE-199",
    "CWE-327", "CWE-91", "CWE-388", "CWE-384", "CWE-417", "CWE-297", "CWE-306")


  def getCatIndex(category: String) = {
    categoryList.indexOf(category)
  }

  def getCategory(i: Int): String = categoryList(i)

  def randomize(random: Random, array: Array[Any]) = {
    var j: Int = array.length - 1
    while (j > 0) {
      val jValue = array(j)
      val rIndex = random.nextInt(j + 1)
      array(j) = array(rIndex)
      array(rIndex) = jValue
      j = j - 1
    }
    array
  }

  def genTrainCsv(output: String, conn: Connection) = {
    val sep = ","
    val data = RnnData.getOutputVector(conn, output).map(line => {
      line(0) = line(0).drop(1).dropRight(1)
      line
    })
    val trainData: Array[String] = output match {
      case "category" => {
        data.map(line => {
          val vector = line(0)
          val category = RnnUtils.getCatIndex(line(1))
          vector + sep + category
        })
      }

      case _ => Array()
    }

    val pw = new PrintWriter(s"data/rnn/train/$output.csv")

    trainData.foreach(line => {
      pw.println(line)
    })

    pw.close()

  }

}
