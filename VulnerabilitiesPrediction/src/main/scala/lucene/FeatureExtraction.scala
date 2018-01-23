package lucene

import java.io._
import util.Utils._

/**
  * Created by ReggieYang on 2016/10/24.
  */
object FeatureExtraction {

  def summaryToFrequencyDir(dirPath: String): Unit = {
    val outputPath = dirPath + "2\\"
    val dir = new File(dirPath)
    dir.listFiles().foreach(file => {
      summaryToFrequency(file.getPath, outputPath + file.getName)
    })
  }

  def summaryToFrequency(filePath: String, outputPath: String): Unit = {
    val summary = scala.io.Source.fromFile(new File(filePath)).getLines().toArray.mkString(" ")
    val wf = LuceneUtils.getWordsFrequency(summary)

    val bw = new BufferedWriter(new FileWriter(new File(outputPath)))
    wf.foreach(x => {
      bw.write(x._1 + TabSep + x._2)
      bw.newLine()
    })
    bw.close()
  }

}
