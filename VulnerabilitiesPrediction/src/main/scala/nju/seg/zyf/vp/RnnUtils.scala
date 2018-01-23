package nju.seg.zyf.vp

import scala.collection.{ SeqView, mutable }
import scala.language.{ higherKinds, implicitConversions }
import scala.util.matching.Regex

import java.util
import java.util.regex.Matcher
import java.util.stream.Collectors
import javax.annotation.{ CheckReturnValue, Nonnegative, Nonnull, ParametersAreNonnullByDefault }

import com.google.common.base.Joiner

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object RnnUtils {

  final case class EncoderStateTensorData[@specialized TData](@Nonnull stateCVectors: Vector[Vector[TData]],
                                                              @Nonnull stateHVectors: Vector[Vector[TData]]) {
    require(this.stateCVectors != null && this.stateHVectors != null)
    require(this.stateCVectors.size == this.stateHVectors.size)

    @Nonnegative @CheckReturnValue
    def size: Int = this.stateCVectors.size

    @Nonnull @CheckReturnValue
    private[RnnUtils] def limitSizeTo(sizeLimit: Int): EncoderStateTensorData[TData] = {
      // assert(sizeLimit > 0 && sizeLimit < this.size)

      EncoderStateTensorData(this.stateCVectors.take(sizeLimit), this.stateHVectors.take(sizeLimit))
    }
  }

  final case class EncoderStateData[@specialized TData](@Nonnegative rnnLayerNum: Int,
                                                        @Nonnegative rnnUnitNum: Int,
                                                        stateData: Vector[EncoderStateTensorData[TData]]) {
    require(this.rnnUnitNum > 0 && this.rnnLayerNum > 0)
    require(this.stateData.length == this.rnnLayerNum)
    // stateData foreach { tensorData => require(tensorData.stateC.size == this.rnnUnitNum) }

    type ContentVectorType = Vector[TData]

    @Nonnegative @CheckReturnValue
    def stateDimension: Int = this.rnnLayerNum * this.rnnUnitNum * 2

    @Nonnegative @CheckReturnValue
    def size: Int = this.stateData(0).stateCVectors.size

    /** Returns a content vector that contains all tensor data at index `i`. */
    @Nonnull @CheckReturnValue
    def contentVectorAt(i: Int): ContentVectorType = {
      require(i < this.size)

      this.stateData.view
      .flatMap { s => Iterable(s.stateCVectors, s.stateHVectors) } // flat map to stateC and stateV
      .flatMap { sv => sv(i) } // flat map to the content vector at index `i`
      .toVector
    }

    /** Returns a vector that contains all tensor data at index `i`. */
    @Nonnull @CheckReturnValue
    def apply(i: Int): ContentVectorType = this.contentVectorAt(i)

    /** Returns all content vectors as a seq view. */
    @Nonnull @CheckReturnValue
    def contentVectors: SeqView[ContentVectorType, Seq[_]] = Range(0, this.size).view map this.contentVectorAt

    @Nonnull @CheckReturnValue
    def limitSizeTo(sizeLimit: Int): EncoderStateData[TData] = {
      require(sizeLimit > 0)

      if (sizeLimit >= this.size) {
        this
      } else {
        this.copy(stateData = this.stateData map { tensor => tensor.limitSizeTo(sizeLimit) })
        //        val limitedStateData = this.stateData map { (tensor: EncoderStateTensorData[TData]) =>
        //          EncoderStateTensorData(tensor.stateCVectors.take(sizeLimit), tensor.stateHVectors.take(sizeLimit))
        //        }
        //        this.copy(stateData = limitedStateData)
      }
    }
  }

  @Nonnull @CheckReturnValue
  def parseRnnLog[TData](logLines: Iterable[String],
                         @Nonnegative rnnLayerNum: Int,
                         @Nonnegative rnnUnitNum: Int,
                         dataParseFunc: String => TData)
  : EncoderStateData[TData] = {
    require(logLines != null && logLines.size >= rnnLayerNum * 2)
    require(rnnUnitNum > 0 && rnnLayerNum > 0)
    require(dataParseFunc != null)

    // generate tensor names (each layer has a tensor with two state, `c` and `h`)
    val encoderStateTensorNames: Vector[String] = Vector.range(0, rnnLayerNum) flatMap { index =>
      Vector(s"${ encoderStateTensorNamePrefix }${ index }C",
             s"${ encoderStateTensorNamePrefix }${ index }H")
    }

    // map names to line start regexs
    val encoderStateTensorLineStartRegexs: Vector[Regex] =
      encoderStateTensorNames map { name => raw"${ name }\s\=\s\[\[".r }

    val encoderStateTensorLineStartMatchers: Vector[Matcher] =
      encoderStateTensorLineStartRegexs map { _.pattern.matcher("") }

    val encoderStateTensorLineEndRegex: Regex = raw"\]\]".r
    val encoderStateTensorLineEndMatcher: Matcher = encoderStateTensorLineEndRegex.pattern.matcher("")

    // Note: Each tensor can have several logs, and each log can have several lines.
    // store logs of each tensor
    val tensorLogsBuilders: Vector[mutable.Builder[String, Vector[String]]] = Vector.fill(rnnLayerNum * 2) { Vector.newBuilder[String] }

    var tensorLogBlockBuilder: mutable.Builder[String, Vector[String]] = null
    var tensorIndex: Int = -1
    var isInBlock: Boolean = false

    for (logLine <- logLines) {
      def handleEndLine(endLine: String): Unit = {
        assert(isInBlock && tensorIndex >= 0)
        val tensorLogBlock: Vector[String] = tensorLogBlockBuilder.result()
        // the log is made by join the lines
        val tensorLogRaw: String = tensorLogBlock.mkString
        val tensorLogStart = tensorLogRaw.indexOf("[[")
        val tensorLogEnd = tensorLogRaw.indexOf("]]")
        tensorLogsBuilders(tensorIndex) += tensorLogRaw.substring(tensorLogStart + 1, tensorLogEnd + 1)
        isInBlock = false
        tensorIndex = -1
      }

      logLine match {
        case endLine if isInBlock && encoderStateTensorLineEndMatcher.reset(endLine).find => // if this is the end line of a block
          tensorLogBlockBuilder += endLine
          handleEndLine(endLine)
        case blockLine if isInBlock                                                       => // if this is a line in a block
          tensorLogBlockBuilder += blockLine
        case _                                                                            =>
          val startLineRegexIndex = encoderStateTensorLineStartMatchers indexWhere { matcher => matcher.reset(logLine).find }
          if (startLineRegexIndex >= 0) {
            // we find a start line for a new log
            // create a new builder
            tensorLogBlockBuilder = Vector.newBuilder[String]
            tensorLogBlockBuilder += logLine
            isInBlock = true
            tensorIndex = startLineRegexIndex

            // as a start line can also be an end line, we must check it here
            if (encoderStateTensorLineEndMatcher.reset(logLine).find) {
              handleEndLine(logLine)
            }
          } // skip other lines
      }
    }

    // build each tensor's log by join log blocks
    val tensorLogs: Vector[String] = tensorLogsBuilders map { _.result().mkString }
    // check we get all logs
    tensorLogs foreach { log => require(log.nonEmpty) }

    val stateDataVectorVectors: Vector[Vector[Vector[TData]]] = tensorLogs map { log =>
      implicit def arrayToScalaVector[T](array: Array[T]): Vector[T] = Vector(array: _*)

      // `rawVectors` contains all data of a tensor
      // call `dataString.substring(1, dataString.length - 1)` to remove the first `[` and the last `]`,
      // and then spilt the vectors by `][`
      val rawVectors: Vector[String] = RnnUtils.splitVectorRegex.split(log.substring(1, log.length - 1))

      // spilt each data (a vector), and use `dataParseFunc` to parse raw data string to data
      val vectorVector: Vector[Vector[TData]] = rawVectors map { rawVectorString =>
        val rawData: Vector[String] = RnnUtils.splitVectorContentRegex.split(rawVectorString)
        rawData map dataParseFunc
      }
      vectorVector
    }
    assert(stateDataVectorVectors.length == rnnLayerNum * 2)

    val encoderStateTensorDataVector: Vector[EncoderStateTensorData[TData]] =
      Vector.range(0, rnnLayerNum) map { index => EncoderStateTensorData[TData](stateDataVectorVectors(index * 2), stateDataVectorVectors(index * 2 + 1)) }

    EncoderStateData(rnnLayerNum, rnnUnitNum, encoderStateTensorDataVector)
  }

  @Nonnull @CheckReturnValue
  def parseRnnLog(logLines: Iterable[String],
                  @Nonnegative rnnLayerNum: Int,
                  @Nonnegative rnnUnitNum: Int)
  : EncoderStateData[Double] =
    RnnUtils.parseRnnLog[Double](logLines, rnnLayerNum, rnnUnitNum, java.lang.Double.parseDouble)

  /**
    * @deprecated Only handle cases when a log record is in one line.
    */
  @Nonnull @CheckReturnValue
  def parseRnnLogSimple[TData](logLines: Iterable[String],
                               @Nonnegative rnnLayerNum: Int,
                               @Nonnegative rnnUnitNum: Int,
                               dataParseFunc: String => TData)
  : EncoderStateData[TData] = {
    require(logLines != null && logLines.size >= rnnLayerNum * 2)
    require(rnnUnitNum > 0 && rnnLayerNum > 0)
    require(dataParseFunc != null)

    // generate tensor names (each layer has a tensor with two state, `c` and `h`)
    val encoderStateTensorNames: Vector[String] = Vector.range(0, rnnLayerNum) flatMap { index =>
      Vector(s"${ encoderStateTensorNamePrefix }${ index }C",
             s"${ encoderStateTensorNamePrefix }${ index }H")
    }

    implicit def arrayToScalaVector[T](array: Array[T]): Vector[T] = Vector(array: _*)

    // map names to regexs
    val encoderStateTensorLineRegexs: Vector[Regex] = encoderStateTensorNames map { name => raw".*${ name }\s\=\s\[(\[.+?\])\].*".r }

    // pick up matched lines
    val stateDataLinesArray: Array[String] = new Array[String](rnnLayerNum * 2)
    for {logLine <- logLines
         (regex, index) <- encoderStateTensorLineRegexs.zipWithIndex
    } {
      logLine match {
        case regex(data) => stateDataLinesArray(index) = data // we find a data line, put it in the Vector
        case _           => // do nothing
      }
    }
    // check we get all data lines
    for (stateDataLine <- stateDataLinesArray) {
      require(stateDataLine != null)
    }

    val stateDataLines: Vector[String] = stateDataLinesArray

    val stateDataVectorVectors: Vector[Vector[Vector[TData]]] = stateDataLines map { dataString =>
      // call `dataString.substring(1, dataString.length - 1)` to remove the first `[` and the last `]`,
      // and then spilt the vectors by `][`
      val rawVectors: Vector[String] = RnnUtils.splitVectorRegex.split(dataString.substring(1, dataString.length - 1))

      // spilt vector content, and use `dataParseFunc` to parse raw data string to data
      val vectorVector: Vector[Vector[TData]] = rawVectors map { rawVectorString =>
        val rawData: Vector[String] = RnnUtils.splitVectorContentRegex.split(rawVectorString)
        rawData map dataParseFunc
      }

      vectorVector
    }
    assert(stateDataVectorVectors.length == rnnLayerNum * 2)

    val encoderStateTensorDataVector: Vector[EncoderStateTensorData[TData]] =
      Vector.range(0, rnnLayerNum) map { index => EncoderStateTensorData[TData](stateDataVectorVectors(index * 2), stateDataVectorVectors(index * 2 + 1)) }

    EncoderStateData(rnnLayerNum, rnnUnitNum, encoderStateTensorDataVector)
  }

  @Nonnull @CheckReturnValue
  def parseRnnLogForce[TData](logLines: Iterable[String],
                              @Nonnegative rnnLayerNum: Int,
                              @Nonnegative rnnUnitNum: Int,
                              dataParseFunc: String => TData)
  : EncoderStateData[TData] = {
    require(logLines != null && logLines.size >= rnnLayerNum * 2)
    require(rnnUnitNum > 0 && rnnLayerNum > 0)
    require(dataParseFunc != null)

    // generate tensor names (each layer has a tensor with two state, `c` and `h`)
    val encoderStateTensorNames: Vector[String] = Vector.range(0, rnnLayerNum) flatMap { index =>
      Vector(s"${ encoderStateTensorNamePrefix }${ index }C",
             s"${ encoderStateTensorNamePrefix }${ index }H")
    }

    implicit def arrayToScalaVector[T](array: Array[T]): Vector[T] = Vector(array: _*)

    // map names to regexs
    val encoderStateTensorLineRegexs: Vector[Regex] = encoderStateTensorNames map { name => raw"${ name }\s\=\s\[(\[.+?\])\]".r }

    // Note: Each tensor can have several logs, and each log can have several lines.
    // store logs of each tensor
    val tensorLogsBuilders: Vector[mutable.Builder[String, Vector[String]]] = Vector.fill(rnnLayerNum * 2) { Vector.newBuilder[String] }

    // contact all log lines
    val logString: String = logLines.mkString

    for ((encoderStateTensorLineRegex, index) <- encoderStateTensorLineRegexs.zipWithIndex) {
      val matcher = encoderStateTensorLineRegex.pattern.matcher(logString)
      while (matcher.find()) {
        val logBlock = matcher.group(1)
        tensorLogsBuilders(index) += logBlock
      }
    }

    // build each tensor's log by join log blocks
    val tensorLogs: Vector[String] = tensorLogsBuilders map { _.result().mkString }
    // check we get all logs
    tensorLogs foreach { log => require(log.nonEmpty) }

    val stateDataVectorVectors: Vector[Vector[Vector[TData]]] = tensorLogs map { log =>
      implicit def arrayToScalaVector[T](array: Array[T]): Vector[T] = Vector(array: _*)

      // `rawVectors` contains all data of a tensor
      // call `dataString.substring(1, dataString.length - 1)` to remove the first `[` and the last `]`,
      // and then spilt the vectors by `][`
      val rawVectors: Vector[String] = RnnUtils.splitVectorRegex.split(log.substring(1, log.length - 1))

      // spilt each data (a vector), and use `dataParseFunc` to parse raw data string to data
      val vectorVector: Vector[Vector[TData]] = rawVectors map { rawVectorString =>
        val rawData: Vector[String] = RnnUtils.splitVectorContentRegex.split(rawVectorString)
        rawData map dataParseFunc
      }
      vectorVector
    }
    assert(stateDataVectorVectors.length == rnnLayerNum * 2)

    val encoderStateTensorDataVector: Vector[EncoderStateTensorData[TData]] =
      Vector.range(0, rnnLayerNum) map { index => EncoderStateTensorData[TData](stateDataVectorVectors(index * 2), stateDataVectorVectors(index * 2 + 1)) }

    EncoderStateData(rnnLayerNum, rnnUnitNum, encoderStateTensorDataVector)
  }

  @Nonnull @CheckReturnValue
  def parseRnnLogForce(logLines: Iterable[String],
                       @Nonnegative rnnLayerNum: Int,
                       @Nonnegative rnnUnitNum: Int)
  : EncoderStateData[Double] =
    RnnUtils.parseRnnLogForce[Double](logLines, rnnLayerNum, rnnUnitNum, java.lang.Double.parseDouble)

  @Nonnull
  private val encoderStateTensorNamePrefix: String = "EncodeState"

  @Nonnull
  private val splitVectorRegex: Regex = raw"\]\[".r // the vectors are spilt by `][`

  @Nonnull
  private val splitVectorContentRegex: Regex = raw"\s".r // the vector content are spilt by whitespace

  @Nonnull @CheckReturnValue
  def summaryToSentence(summary: String, stringLengthLimit: Int): String = {
    require(summary != null)
    require(stringLengthLimit > 0)

    // take `stringLengthLimit` chars from `summary`
    if (summary.length <= stringLengthLimit) summary
    else if (summary.length <= stringLengthLimit * 2) summary.takeRight(stringLengthLimit)
    else summary.substring(stringLengthLimit, stringLengthLimit * 2)
  }

  @Nonnull @CheckReturnValue
  def summaryToSentenceWithWordLimit1(summary: String,
                                      wordLengthLimit: Int,
                                      wordSeparatorRegex: Regex = raw"\s".r)
  : String = {
    require(summary != null)
    require(wordLengthLimit > 0)
    require(wordSeparatorRegex != null)

    val summaryWords = wordSeparatorRegex.split(summary)

    val limitedSummaryWords: Array[String] =
      if (summaryWords.length <= wordLengthLimit) summaryWords
      else if (summaryWords.length <= wordLengthLimit * 2) summaryWords.takeRight(wordLengthLimit) // take last `wordLengthLimit` words
      else summaryWords.slice(wordLengthLimit, wordLengthLimit + wordLengthLimit) // drop first `wordLengthLimit` words, and then take `wordLengthLimit` words

    limitedSummaryWords.mkString(" ")
    // Joiner.on(" ").join(limitedSummaryWords.asInstanceOf[Array[Object]])
  }

  @Nonnull @CheckReturnValue
  def summaryToSentenceWithWordLimit2(summary: String,
                                      wordLengthLimit: Int,
                                      wordSeparatorRegex: Regex = raw"\s".r)
  : String = {
    require(summary != null)
    require(wordLengthLimit > 0)
    require(wordSeparatorRegex != null)

    val summaryWords: Array[String] = wordSeparatorRegex.split(summary)
    import com.google.common.collect.TreeMultiset
    val summaryWordsMultiset: TreeMultiset[String] = TreeMultiset.create(java.util.Arrays.asList(summaryWords: _*))
    // get descending set, in which most frequently occurring words come first
    val distinctSummaryWords: util.NavigableSet[String] = summaryWordsMultiset.elementSet.descendingSet

    val limitedSummaryWords: util.Collection[String] =
      if (distinctSummaryWords.size <= wordLengthLimit) distinctSummaryWords
      else distinctSummaryWords.stream.limit(wordLengthLimit).collect(Collectors.toList[String]) // take `wordLengthLimit` most frequently occurring words

    Joiner.on(" ").join(limitedSummaryWords)
  }
}
