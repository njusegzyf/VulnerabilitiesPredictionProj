package nju.seg.zyf.vp.app

import java.io.IOException
import java.nio.file.{ Files, Path }
import javax.annotation.ParametersAreNonnullByDefault

import com.google.common.base.{ Charsets, Strings }
import com.google.common.io.{ Files => GuavaFiles }

import org.slf4j.{ Logger, LoggerFactory }

import nju.seg.zyf.vp.{ SqlUtils, Utils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp2SaveNmtInputApp extends App {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  import ExpConfigs.mySQLConnectionConfig // import as an implicit val

  this.saveProductSummaryAndCategory(Exp2Configs.exp2PathConfig.trainDataDir,
                                     Exp2Configs.exp2CategoryPrefix,
                                     Exp2Configs.exp2DataInstancesSizeLimit,
                                     summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                     skipWhenFileExisted = Exp2Configs.exp2SkipWhenFileExisted)

  this.saveProductSummaryAndAmount(Exp2Configs.exp2PathConfig.trainDataDir,
                                   Exp2Configs.exp2AmountPrefix,
                                   Exp2Configs.exp2DataInstancesSizeLimit,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp2Configs.exp2SkipWhenFileExisted)

  this.saveProductSummaryAndImpact(Exp2Configs.exp2PathConfig.trainDataDir,
                                   Exp2Configs.exp2ImpactPrefix,
                                   Exp2Configs.exp2DataInstancesSizeLimit,
                                   summaryTransFunc = ExpConfigs.defaultSentenceTransFunc,
                                   skipWhenFileExisted = Exp2Configs.exp2SkipWhenFileExisted)

  @throws[IOException]
  private def saveProductSummaryAndCategory(saveDir: Path,
                                    filePrefix: String,
                                    sizeLimit: Int,
                                    summaryTransFunc: String => String = ExpConfigs.defaultSentenceTransFunc,
                                    skipWhenFileExisted: Boolean = true)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))

    def saveProductSummaryAndCategoryForOneCombination(keyCombination: String): Unit = {
      this.logger.info(s"Start processing key combination: $keyCombination.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip key combination: $keyCombination as the files already existed.")
        return
      }

      val tuples: Vector[(String, String)] =
        SqlUtils.getProductSummaryAndCategory(s"search_res_${ keyCombination }",
                                              "vul_category",
                                              sizeLimit,
                                              productSummaryFieldInProductSummaryTable = "res")
      val summaries = tuples map { _._1 }
      val categories = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write categories to the file
      val categoryFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Category.txt")
      GuavaFiles.asCharSink(categoryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(categories)
    }

    for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
      saveProductSummaryAndCategoryForOneCombination(keyCombination)
    }
  }

  @throws[IOException]
  private def saveProductSummaryAndAmount(saveDir: Path,
                                  filePrefix: String,
                                  sizeLimit: Int,
                                  summaryTransFunc: String => String = ExpConfigs.defaultSentenceTransFunc,
                                  skipWhenFileExisted: Boolean = true)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))

    def saveProductSummaryAndAmountForOneCombination(keyCombination: String): Unit = {
      this.logger.info(s"Start processing key combination: $keyCombination.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip key combination: $keyCombination as the files already existed.")
        return
      }

      val tuples: Vector[(String, Double)] =
        SqlUtils.getProductSummaryAndAmount(s"search_res_${ keyCombination }",
                                            "vul_amount",
                                            sizeLimit,
                                            productSummaryFieldInProductSummaryTable = "res")
      val summaries = tuples map { _._1 }
      val amount = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write amounts to the file
      val amountFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Amount.txt")
      GuavaFiles.asCharSink(amountFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(amount map { _.toString })
    }

    for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
      saveProductSummaryAndAmountForOneCombination(keyCombination)
    }
  }

  @throws[IOException]
  private def saveProductSummaryAndImpact(saveDir: Path,
                                  filePrefix: String,
                                  sizeLimit: Int,
                                  summaryTransFunc: String => String = ExpConfigs.defaultSentenceTransFunc,
                                  skipWhenFileExisted: Boolean = true)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(filePrefix))

    def saveProductSummaryAndImpactForOneCombination(keyCombination: String): Unit = {
      this.logger.info(s"Start processing key combination: $keyCombination.")

      val summaryFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Summary.txt")
      if (skipWhenFileExisted && Files.exists(summaryFile)) {
        this.logger.info(s"Skip key combination: $keyCombination as the files already existed.")
        return
      }

      val tuples: Vector[(String, Double)] =
        SqlUtils.getProductSummaryAndImpact(s"search_res_${ keyCombination }",
                                            "vul_impact",
                                            sizeLimit,
                                            productSummaryFieldInProductSummaryTable = "res")
      val summaries = tuples map { _._1 }
      val impact = tuples map { _._2 }

      // write summaries to the file as nmt infer input file
      import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

      // write impacts to the file
      val impactFile: Path = saveDir.resolve(s"${ filePrefix }_${ keyCombination }_Data_Impact.txt")
      GuavaFiles.asCharSink(impactFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(impact map { _.toString })
    }

    for (keyCombination <- Exp2Configs.exp2AllKeyCombinations) {
      saveProductSummaryAndImpactForOneCombination(keyCombination)
    }
  }
}
