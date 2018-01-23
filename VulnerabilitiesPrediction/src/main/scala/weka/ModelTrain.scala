package weka

import java.io.{FileOutputStream, ObjectOutputStream, PrintWriter}
import java.util.Random

import org.slf4j.LoggerFactory
import weka.classifiers.{Classifier, Evaluation}
import weka.classifiers.meta.MultiClassClassifier
import weka.classifiers.trees.RandomForest
import weka.core.{Instances, SerializationHelper}
import weka.core.converters.ConverterUtils.{DataSink, DataSource}
import weka.filters.Filter
import weka.filters.supervised.attribute.Discretize
import weka.filters.unsupervised.attribute.StringToNominal

/**
  * Created by ReggieYang on 2017/3/15.
  */
class ModelTrain {

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  def preprocess(filePath: String, filter: Filter): String = {
    val testSet = DataSource.read(filePath)
    filter.setInputFormat(testSet)
    val filteredFilePath = filePath.replaceAll("(.*//)(.*)(/.arff)", "$1$2_nominal$3")
    DataSink.write(filteredFilePath, Filter.useFilter(testSet, filter))
    filteredFilePath
  }

  def trainModel(filePath: String, cls: Classifier, clsPath: String, classIndex: Int) = {
    logger.info("Begin training...")
    val instances = DataSource.read(filePath)
    instances.setClassIndex(classIndex)
    cls.buildClassifier(instances)
    logger.info("Training completed. Begin saving...")
    SerializationHelper.write(clsPath, cls)
//    val oos = new ObjectOutputStream(new FileOutputStream(clsPath))
//    oos.writeObject(cls)
    logger.info("Saving completed.")
//    oos.flush()
//    oos.close()
  }

  def crossValidate2(output: String, emptyCls: Classifier = new RandomForest, remark:String = "") = {
    val algorithmName = emptyCls.getClass.getSimpleName
    val modelType = if (output == "category") "classification" else "regression"

    val trainDataPath = s"data/wekaData/train2/${output}_$remark.arff"
    val cls = emptyCls
    val trainData = DataSource.read(trainDataPath)
    val classIndex = trainData.numAttributes() - 1
    trainData.setClassIndex(classIndex)
    logger.info("training: " + algorithmName)
    //Read the training data

    val finalClassifier = emptyCls
    logger.info("Start training...")
    finalClassifier.buildClassifier(trainData)

    new RandomForest
    logger.info("Start saving classifier...")
    SerializationHelper.write(s"data/wekaData/model2/${output}_${algorithmName}_$remark.cls", finalClassifier)
    //Train and save the model

    val folds = 10
    val rand = new Random(1)
    trainData.setClassIndex(trainData.numAttributes() - 1)
    val randData = new Instances(trainData)
    randData.randomize(rand)
    randData.stratify(folds)
    val eval = new Evaluation(randData)
    logger.info("Start evaluating...")

    val pw = new PrintWriter(s"data/wekaData/evaluation2/accuracy_measure_${output}_${algorithmName}_$remark.txt")

    if (modelType == "classification") {
      val fRanks = Range(0, folds).map(i => {
        val train = randData.trainCV(folds, i)
        val test = randData.testCV(folds, i)
        cls.buildClassifier(train)
        eval.evaluateModel(cls, test)
        val fRank = WekaUtils.calFRank(cls, test)
        pw.println(s"FRank$i: " + fRank)
        logger.info(s"FRank$i: " + fRank)
        fRank
      })
      SerializationHelper.write(s"data/wekaData/evaluation2/evaluation_${output}_${algorithmName}_$remark.eval", eval)
      logger.info(eval.toSummaryString())
      pw.println("Average FRank: " + fRanks.sum / fRanks.size)
      logger.info("Average FRank: " + fRanks.sum / fRanks.size)
      pw.close()
    }
    else {
      val CREs = Range(0, folds).map(i => {
        val train = randData.trainCV(folds, i)
        val test = randData.testCV(folds, i)
        cls.buildClassifier(train)
        eval.evaluateModel(cls, test)
        val cre = WekaUtils.calCRE(cls, test)
        val cre2 = WekaUtils.calCRE2(cls, test)
        pw.println(s"CCRE$i: " + cre)
        pw.println(s"CRE$i: " + cre2)
        logger.info(s"CCRE$i: " + cre)
        logger.info(s"CRE$i: " + cre2)
        (cre, cre2)
      })
      SerializationHelper.write(s"data/wekaData/evaluation2/evaluation_${output}_${algorithmName}_$remark.eval", eval)
      logger.info(eval.toSummaryString())
      val ccres = CREs.map(_._1)
      val cres = CREs.map(_._2)

      pw.println("Avg CCRE: " + ccres.sum / ccres.size)
      pw.println("Avg CRE: " + cres.sum / cres.size)
      logger.info("Avg CCRE: " + ccres.sum / ccres.size)
      logger.info("Avg CRE: " + cres.sum / cres.size)
      pw.close()

    }
    //    val eval = new Evaluation(trainData)
    //    logger.info("Start crossValidating...")
    //    eval.crossValidateModel(cls, trainData, 10, new Random(1))
    //    logger.info(eval.toSummaryString())
    //    logger.info("Saving evaluations...")
    //    SerializationHelper.write(s"data/wekaData/evaluation/evaluation_${output}_$algorithmName.eval", eval)
    //Evaluate the model using cross-validation
  }

  def crossValidate(output: String, emptyCls: Classifier = new MultiClassClassifier, modelType: String = "classification") = {
    val algorithmName = emptyCls.getClass.getSimpleName

    val trainDataPath = s"data/wekaData/train/${output}_$modelType.arff"
    val cls = emptyCls
    val trainData = DataSource.read(trainDataPath)
    val classIndex = trainData.numAttributes() - 1
    trainData.setClassIndex(classIndex)
    logger.info("training: " + algorithmName)
    //Read the training data

    val finalClassifier = emptyCls
    logger.info("Start training...")
    finalClassifier.buildClassifier(trainData)
    logger.info("Start saving classifier...")
    SerializationHelper.write(s"data/wekaData/model/${output}_$algorithmName.cls", finalClassifier)
    //Train and save the model

    val folds = 10
    val rand = new Random(1)
    trainData.setClassIndex(trainData.numAttributes() - 1)
    val randData = new Instances(trainData)
    randData.randomize(rand)
    randData.stratify(folds)
    val eval = new Evaluation(randData)
    logger.info("Start evaluating...")

    if (modelType == "classification") {
      val fRanks = Range(0, 1).map(i => {
        val train = randData.trainCV(folds, i)
        val test = randData.testCV(folds, i)
        cls.buildClassifier(train)
        eval.evaluateModel(cls, test)
        val fRank = WekaUtils.calFRank(cls, test)
        logger.info(s"FRank$i: " + fRank)
        fRank
      })
      SerializationHelper.write(s"data/wekaData/evaluation/evaluation_${output}_$algorithmName.eval", eval)
      logger.info(eval.toSummaryString())
      logger.info("FRank: " + fRanks.sum / fRanks.size)
    }
    else {
      val CREs = Range(0, folds).map(i => {
        val train = randData.trainCV(folds, i)
        val test = randData.testCV(folds, i)
        cls.buildClassifier(train)
        eval.evaluateModel(cls, test)
        val cre = WekaUtils.calCRE(cls, test)
        val cre2 = WekaUtils.calCRE2(cls, test)
        logger.info(s"CCRE$i: " + cre)
        logger.info(s"CRE$i: " + cre2)
        (cre, cre2)
      })
      SerializationHelper.write(s"data/wekaData/evaluation/evaluation_${output}_$algorithmName.eval", eval)
      logger.info(eval.toSummaryString())
      val ccres = CREs.map(_._1)
      val cres = CREs.map(_._2)

      logger.info("CCRE: " + ccres.sum / ccres.size)
      logger.info("CRE: " + cres.sum / cres.size)

    }
    //    val eval = new Evaluation(trainData)
    //    logger.info("Start crossValidating...")
    //    eval.crossValidateModel(cls, trainData, 10, new Random(1))
    //    logger.info(eval.toSummaryString())
    //    logger.info("Saving evaluations...")
    //    SerializationHelper.write(s"data/wekaData/evaluation/evaluation_${output}_$algorithmName.eval", eval)
    //Evaluate the model using cross-validation
  }


}
