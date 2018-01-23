package w2v

import java.io.File

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory



/**
  * Created by ReggieYang on 2016/10/25.
  */
object Word2VecUtils {

  def main(args: Array[String]) = {

    val filePath = new File("data\\desc.txt").getAbsolutePath
    val iter = new BasicLineIterator(filePath)

    val t = new DefaultTokenizerFactory
    t.setTokenPreProcessor(new CommonPreprocessor)
    val vec = new Word2Vec.Builder().minWordFrequency(5).iterations(1).layerSize(100)
      .seed(42).windowSize(5).iterate(iter).tokenizerFactory(t).build()

    vec.fit()

    val list = vec.wordsNearest("happy", 5)
    println(list)

//    val gModel = new File("E:\\Download\\GoogleNews-vectors-negative300.bin.gz")
//    val vec = WordVectorSerializer.loadGoogleModel(gModel, true)


    WordVectorSerializer.writeWordVectors(vec.asInstanceOf[Word2Vec], "E:\\Download\\1.txt")
//    println(vec.wordsNearest("terminate", 10))
  }

}
