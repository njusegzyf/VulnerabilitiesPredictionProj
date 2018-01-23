package rnn

import java.io.File
import java.sql.Connection
import java.util.Random

import nvd.data.DBConnection
import org.datavec.api.records.reader.impl.csv.CSVRecordReader
import org.datavec.api.split.FileSplit
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.nn.conf.{GradientNormalization, NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.conf.layers.{GravesLSTM, RnnOutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.lossfunctions.LossFunctions
import w2v.W2VRNN.logger

/**
  * Created by kaimaoyang on 2017/5/12.
  */
class RnnWorkFlow(word2VecPath: String) {

  def trainModel() = {
    val batchSize = 1 //Number of examples in each minibatch
    val vectorSize = 100 //Size of the word vectors. 300 in the Google News model
    val nEpochs = 1 //Number of epochs (full passes of training data) to train on
    val truncateReviewsToLength = 1024 //Truncate reviews with length (# words) greater than this

    val catNum = RnnUtils.categoryList.length

    val conf = new NeuralNetConfiguration.Builder()
      .updater(Updater.ADAM).adamMeanDecay(0.9).adamVarDecay(0.999)
      .regularization(true).l2(1e-5)
      .weightInit(WeightInit.XAVIER)
      .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue).gradientNormalizationThreshold(1.0)
      .learningRate(1e-3)
      .list()
      .layer(0, new GravesLSTM.Builder().nIn(vectorSize).nOut(256)
        .activation(Activation.TANH).build())
      .layer(1, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(catNum).build())
      .pretrain(false).backprop(true).build()

    val net = new MultiLayerNetwork(conf)
    net.init()
    net.setListeners(new ScoreIterationListener(1))
    val wordVectors = WordVectorSerializer.readParagraphVectors(new File(word2VecPath))

    val conn = DBConnection.getConnection
    val summary = RnnData.getSum4Cat(conn).take(16384)
    val expOutput = RnnData.getCat(conn).take(16384)
    conn.close()

    val train = new SummaryIterator(wordVectors, batchSize,
      truncateReviewsToLength, summary, expOutput, catNum)

    Range(0, nEpochs).foreach(i => {
      logger.info(s"Start training: Epoch $i")
      net.fit(train)
      train.reset()
    })

    ModelSerializer.writeModel(net, "data/rnn/netCategory.net", true)
  }


  def evaluate() = {
    val batchSize = 1 //Number of examples in each minibatch
    val truncateReviewsToLength = 1024 //Truncate reviews with length (# words) greater than this

    val wordVectors = WordVectorSerializer.loadStaticModel(new File(word2VecPath))

    val conn = DBConnection.getConnection
    val summary = RnnData.getSum4Cat(conn).take(100)
    val expOutput = RnnData.getCat(conn).take(100)
    //    val sf: Array[Any] = summary.zip(feature)

    //    val randSF = RnnUtils.randomize(new Random(1), sf).map(_.asInstanceOf[(String, Int)])
    //    val randSum = randSF.map(_._1)
    //    val randFeature = randSF.map(_._2)

    conn.close()

    val test = new SummaryIterator(wordVectors, batchSize, truncateReviewsToLength
      , summary, expOutput, RnnUtils.categoryList.length)

    val net = ModelSerializer.restoreMultiLayerNetwork("data/rnn/netCategory.net")

    var sum = 0
    var acc = 0

    while (test.hasNext) {
      val cursor1 = test.cursor
      val t = test.next()
      val cursor2 = test.cursor

      val features = t.getFeatureMatrix
      val actual = Range(cursor1, cursor2).map(expOutput(_))
      val inMask = t.getFeaturesMaskArray
      val outMask = t.getLabelsMaskArray
      val predicted = net.output(features, false, inMask, outMask)

      val batch = Range(0, batchSize).map(i => {
        predicted.getRow(0).getColumn(predicted.getRow(0).columns() - 1).toString.drop(1).dropRight(1)
          .split(",").map(prob => prob.toDouble).zipWithIndex.sortBy(0 - _._1)
          .map(_._2).indexOf(actual(i)) + 1
      })

      val predSeq = Range(0, batchSize).map(i => {
        predicted.getRow(0).getColumn(predicted.getRow(0).columns() - 1).toString.drop(1).dropRight(1)
          .split(",").map(prob => prob.toDouble).zipWithIndex.sortBy(0 - _._1)
      })


      println("" + RnnUtils.getCategory(actual.head) + "\t" +
        predSeq.head.take(6).map(x => RnnUtils.getCategory(x._2) + "(" + x._1 + ")").mkString(","))

      val a = batch.count(x => x == 1)

      val batchSum = batch.sum
      sum = sum + batchSum
      acc = a + acc
      val avg = batchSum.toDouble / batchSize.toDouble
      //      logger.info(s"batchAvg: $avg")
    }


    val fRank = sum.toDouble / summary.length.toDouble
    //    logger.info(s"sum: $sum")

    val accu = acc.toDouble / summary.length.toDouble
    //    logger.info(s"accu: $accu")

    //    logger.info(s"FRank: $fRank")
    test.reset()

  }

  def trainNN(conn: Connection) = {
    val delimiter = ","
    val recordReader = new CSVRecordReader(0, delimiter)
    recordReader.initialize(new FileSplit(new File("data/rnn/train/category.csv")))

    val labelIndex = 100
    val numClasses = 67
    val batchSize = 150


  }

  def evaluateNN(testData: DataSet) = {
    val model = ModelSerializer.restoreMultiLayerNetwork("data/rnn/nnCategory2.net")

    //evaluate the model on the test set
    val eval = new Evaluation(67)
    val output = model.output(testData.getFeatureMatrix)
    eval.eval(testData.getLabels, output)
    val labels = testData.getLabels
    val cats = Range(0, labels.rows()).map(i => {
      labels.getRow(i).toString.drop(1).dropRight(1).split(",").map(x => x.toDouble).indexOf(1d)
    })

    val batch = Range(0, output.rows()).map(i => {
      output.getRow(i).toString.drop(1).dropRight(1)
        .split(",").map(prob => prob.toDouble).zipWithIndex.sortBy(0 - _._1)
        .map(_._2).indexOf(cats(i))
    })

    val accu = Range(0, output.rows()).map(i => {
      val pre = output.getRow(i).toString.drop(1).dropRight(1)
        .split(",").map(prob => prob.toDouble).zipWithIndex.sortBy(0 - _._1)
        .map(_._2).head
      val exp = cats(i)
      if (pre == exp) 1 else 0
    })

    logger.info("frank: " + batch.sum.toDouble / batch.length.toDouble + 1)
    logger.info("accu: " + accu.sum.toDouble / accu.length.toDouble + 1)


  }

}
