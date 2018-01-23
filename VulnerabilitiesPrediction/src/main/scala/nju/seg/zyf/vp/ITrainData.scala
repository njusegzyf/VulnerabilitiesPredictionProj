package nju.seg.zyf.vp

import scala.collection.SeqView

import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import nju.seg.zyf.vp.ITrainData.Feature
import nju.seg.zyf.vp.RnnUtils.EncoderStateData

/** Represents train data for a feature.
  *
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
sealed trait ITrainData[TIn, TOut] {

  type ContentVectorType = Vector[TIn]

  @Nonnull @CheckReturnValue
  def featureType: Feature.Value

  @Nonnull @CheckReturnValue
  final def featureName: String = this.featureType.toString // `toString` of `Enumeration` returns its name

  @Nonnull @CheckReturnValue
  def rawInput: EncoderStateData[TIn]

  @Nonnull @CheckReturnValue
  def rawOutput: Vector[TOut]

  @Nonnull @CheckReturnValue
  final def data: SeqView[(ContentVectorType, TOut), Seq[_]] =
    this.rawInput.contentVectors zip rawOutput

  @Nonnegative @CheckReturnValue
  final def inputDimension: Int = this.rawInput.stateDimension

  //region Use raw data of form Iterable[(EncoderStateData[TInData], TOut)], deprecated

  //  @deprecated @Nonnull @CheckReturnValue
  //  def rawData: Iterable[(EncoderStateData[TInData], TOut)]
  //
  //  @deprecated @Nonnull @CheckReturnValue
  //  final def data: Iterable[(ContentVectorType, TOut)] =
  //    this.rawData flatMap { (inOutTuple: (EncoderStateData[TInData], TOut)) =>
  //      // flat map all content vectors in the `EncoderStateData` to tuples of `(contentVector, out)`
  //      val (in: EncoderStateData[TInData], out: TOut@unchecked) = inOutTuple
  //      in.contentVectors.map { (contentVector: ContentVectorType) => (contentVector, out) }
  //    }
  //
  //  @deprecated
  //  final def inputDimension: Int = this.rawData.head._1.stateDimension

  //endregion Use raw data of form Iterable[(EncoderStateData[TInData], TOut)], deprecated
}

object ITrainData {

  object Feature extends Enumeration {
    val Category: Feature.Value = Value("Category")
    val Amount: Feature.Value = Value("Amount")
    val Impact: Feature.Value = Value("Impact")
  }

  final case class CategoryTrainData(override val rawInput: EncoderStateData[Double],
                                     override val rawOutput: Vector[String])
    extends ITrainData[Double, String] {

    @Nonnull @CheckReturnValue
    override def featureType: Feature.Value = Feature.Category
  }

  trait ISimpleTrainData extends ITrainData[Double, Double]

  final case class AmountTrainData(override val rawInput: EncoderStateData[Double],
                                   override val rawOutput: Vector[Double])
    extends ISimpleTrainData {

    @Nonnull @CheckReturnValue
    override def featureType: Feature.Value = Feature.Amount
  }

  final case class ImpactTrainData(override val rawInput: EncoderStateData[Double],
                                   override val rawOutput: Vector[Double])
    extends ISimpleTrainData {

    @Nonnull @CheckReturnValue
    override def featureType: Feature.Value = Feature.Impact
  }

}
