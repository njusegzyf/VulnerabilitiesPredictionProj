package lucene

import java.io.{ File, FileReader, StringReader }

import org.apache.lucene.analysis.{ CharArraySet, TokenStream }
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

import collection.JavaConversions._

/**
  * Created by ReggieYang on 2016/10/24.
  */
object LuceneUtils {

  val stopWords = new CharArraySet(scala.io.Source.fromFile(new File("data\\lucene\\stopWords")).getLines().toSet, true)

  def getWords(content: String): Array[String] = {
    val analyzer = new StandardAnalyzer(stopWords)
    val sr = new StringReader(content)
    val ts = analyzer.tokenStream("word", sr)
    val tsI = new TSIterator(ts)
    val words = tsI.toArray
    ts.close()
    words
  }

  def getWordsFromFile(filePath: String): Array[String] = {
    val analyzer = new StandardAnalyzer(stopWords)
    val fr = new FileReader(filePath)
    val ts = analyzer.tokenStream("word", fr)
    val tsI = new TSIterator(ts)
    val words = tsI.toArray
    ts.close()
    words
  }

  def getWordsFrequency(content: String): List[(String, Int)] = {
    //    countFrequency(getWords(content))
    getWords(content).groupBy(x => x).map(y => (y._1, y._2.length)).toList
  }

  def getWordsFrequencyFromFile(filePath: String): List[(String, Int)] = {
    //    countFrequency(getWords(content))
    getWordsFromFile(filePath).groupBy(x => x).map(y => (y._1, y._2.length)).toList
  }

  def countFrequency(words: Array[String]): List[(String, Int)] = {
    val tempMap = scala.collection.mutable.Map[String, Int]()
    words.foreach(word => {
      tempMap.get(word) match {
        case Some(count) => tempMap.put(word, count + 1)
        case _           => tempMap.put(word, 1)
      }
    })
    tempMap.toMap.toList.sortBy(x => -x._2)
  }

  class TSIterator(ts: TokenStream) extends Iterator[String] {
    ts.reset()
    ts.addAttribute(classOf[CharTermAttribute])

    override def next(): String = {
      ts.getAttribute(classOf[CharTermAttribute]).toString
    }

    override def hasNext: Boolean = ts.incrementToken()
  }

}
