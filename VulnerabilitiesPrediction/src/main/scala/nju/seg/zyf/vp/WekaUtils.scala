package nju.seg.zyf.vp

import java.nio.file.{ Files, Path }
import java.util
import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Strings
import com.google.common.collect.Lists

import org.slf4j.{ Logger, LoggerFactory }

import nju.seg.zyf.vp.ITrainData.{ CategoryTrainData, ISimpleTrainData }
import nju.seg.zyf.vp.IWekaMidResult.{ ClassificationResultForInstance, RegressionResultForInstance }
import weka.classifiers.Classifier
import weka.core.converters.ConverterUtils.DataSink
import weka.core.{ Attribute, DenseInstance, Instance, Instances }
import weka.filters.Filter
import weka.filters.unsupervised.attribute.StringToNominal

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object WekaUtils {

  /**
    * @see [[workflow.MainWorkFlow]]
    */
  def saveContentToArffFile(trainData: ITrainData[_, _],
                            saveFolder: Path,
                            saveFileName: String)
  : Unit = {
    require(trainData != null)
    require(saveFolder != null && Files.isDirectory(Files.createDirectories(saveFolder)))
    require(!Strings.isNullOrEmpty(saveFileName))

    // the following code refers to `getTrainData` method in class `workflow.MainWorkFlow`
    val featureName = trainData.featureName
    this.logger.info(s"Begin processing feature: $featureName.")

    val saveFile: Path = saveFolder.resolve(s"$saveFileName.arff")

    val stateDataDimension: Int = trainData.inputDimension
    val attributesBuilder = Vector.newBuilder[Attribute]
    // add attributes for each state dimension
    attributesBuilder ++= Range(0, stateDataDimension) map { i => new Attribute(s"v$i") }

    trainData match {
      case categoryTrainData: CategoryTrainData =>
        val categoryAttribute: Attribute = new Attribute(featureName, null: util.ArrayList[String])
        attributesBuilder += categoryAttribute
        val attributes: util.ArrayList[Attribute] = Lists.newArrayList(attributesBuilder.result(): _*)
        val instances = new Instances(featureName, attributes, 0)

        for ((contentVector, category) <- categoryTrainData.data) {
          // since `category` is a string, we need to convert it to double using a `StringToNominal`
          val categoryAsDouble = categoryAttribute.addStringValue(category).toDouble
          val values: Array[Double] = (contentVector :+ categoryAsDouble).toArray[Double]
          instances.add(new DenseInstance(1.0, values))
        }
        val filter: StringToNominal = new StringToNominal
        filter.setInputFormat(instances)
        DataSink.write(saveFile.toAbsolutePath.toString, Filter.useFilter(instances, filter))

      case simpleTrainData: ISimpleTrainData =>
        attributesBuilder += new Attribute(featureName)
        val attributes: util.ArrayList[Attribute] = Lists.newArrayList(attributesBuilder.result(): _*)
        val instances = new Instances(featureName, attributes, 0)

        for ((contentVector, out) <- simpleTrainData.data) {
          // values are content vector plus out
          val values: Array[Double] = (contentVector :+ out).toArray[Double]
          instances.add(new DenseInstance(1.0, values))
        }
        DataSink.write(saveFile.toAbsolutePath.toString, instances)
    }

    this.logger.info(s"End processing feature: $featureName. Training data is saved at: $saveFile.")
  }

  @Nonnull @CheckReturnValue
  def computeClassificationResult(instance: Instance)
                                 (implicit cls: Classifier)
  : ClassificationResultForInstance = {
    require(instance != null)
    require(cls != null)

    val distribution: Array[Double] = cls.distributionForInstance(instance)
    val classValue = instance.classValue.toInt
    ClassificationResultForInstance(distribution, classValue)
  }

  @Nonnull @CheckReturnValue
  def computeRegressionResult(instance: Instance)
                             (implicit cls: Classifier)
  : RegressionResultForInstance = {
    require(instance != null)
    require(cls != null)

    val pred = cls.classifyInstance(instance)
    val actual = instance.classValue
    RegressionResultForInstance(pred, actual)
  }

  @Nonnull
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
