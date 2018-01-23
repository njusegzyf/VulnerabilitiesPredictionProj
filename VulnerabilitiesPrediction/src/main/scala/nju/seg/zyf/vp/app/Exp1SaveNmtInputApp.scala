package nju.seg.zyf.vp.app

import java.io.IOException
import java.nio.file.Path
import javax.annotation.ParametersAreNonnullByDefault

import com.google.common.base.{ Charsets, Strings }
import com.google.common.io.{ Files => GuavaFiles }

import nju.seg.zyf.vp.{ SqlUtils, Utils }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp1SaveNmtInputApp extends App {

  import ExpConfigs.mySQLConnectionConfig // import as an implicit val

  Exp1SaveNmtInputApp.saveProductSummaryAndCategory(Exp1Configs.exp1PathConfig.trainDataDir,
                                                    s"${ Exp1Configs.exp1CategoryPrefix }_Data_Summary",
                                                    s"${ Exp1Configs.exp1CategoryPrefix }_Data_Category",
                                                    sizeLimit = Exp1Configs.exp1DataInstancesSizeLimit,
                                                    summaryTransFunc = ExpConfigs.defaultSentenceTransFunc)

  //    Exp1SaveNmtInputApp.saveProductSummaryAndCategoryInBatch(Exp1Configs.exp1PathConfig.trainDataDir,
  //                                                             s"${ Exp1Configs.exp1CategoryPrefix }_Data_Summary_Dir",
  //                                                             s"${ Exp1Configs.exp1CategoryPrefix }_Data_Summary",
  //                                                             s"${ Exp1Configs.exp1CategoryPrefix }_Data_Category")

  Exp1SaveNmtInputApp.saveProductSummaryAndAmount(Exp1Configs.exp1PathConfig.trainDataDir,
                                                  s"${ Exp1Configs.exp1AmountPrefix }_Data_Summary",
                                                  s"${ Exp1Configs.exp1AmountPrefix }_Data_Amount",
                                                  sizeLimit = Exp1Configs.exp1DataInstancesSizeLimit,
                                                  summaryTransFunc = ExpConfigs.defaultSentenceTransFunc)

  Exp1SaveNmtInputApp.saveProductSummaryAndImpact(Exp1Configs.exp1PathConfig.trainDataDir,
                                                  s"${ Exp1Configs.exp1ImpactPrefix }_Data_Summary",
                                                  s"${ Exp1Configs.exp1ImpactPrefix }_Data_Impact",
                                                  sizeLimit = Exp1Configs.exp1DataInstancesSizeLimit,
                                                  summaryTransFunc = ExpConfigs.defaultSentenceTransFunc)

  @throws[IOException]
  private def saveProductSummaryAndCategory(saveDir: Path,
                                            summaryFileName: String,
                                            categoryFileName: String,
                                            sizeLimit: Int,
                                            summaryTransFunc: String => String = { s => s })
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(summaryFileName))
    require(!Strings.isNullOrEmpty(categoryFileName))

    val tuples: Vector[(String, String)] = SqlUtils.getProductSummaryAndCategory("product_summary", "vul_category", sizeLimit)
    val summaries = tuples map { _._1 }
    val categories = tuples map { _._2 }

    // write summaries to the file as nmt infer input file
    import scala.collection.JavaConverters.{ asJavaCollection, asJavaIterable }
    val summaryFile: Path = saveDir.resolve(s"$summaryFileName.txt")
    GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

    // write categories to the file
    val categoryFile: Path = saveDir.resolve(s"$categoryFileName.txt")
    GuavaFiles.asCharSink(categoryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(categories)
  }

  @throws[IOException]
  private def saveProductSummaryAndCategoryInBatch(saveDir: Path,
                                                   summaryBatchesDirName: String,
                                                   summaryFileName: String,
                                                   categoryFileName: String,
                                                   batchSize: Int = 1024 * 8)
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(summaryBatchesDirName))
    require(!Strings.isNullOrEmpty(summaryFileName))
    require(!Strings.isNullOrEmpty(categoryFileName))
    require(batchSize > 1024)

    val tuples: Vector[(String, String)] = SqlUtils.getProductSummaryAndCategory("product_summary", "vul_category")
    val summaries = tuples map { _._1 }
    val categories = tuples map { _._2 }

    val summaryBatchesDir = saveDir.resolve(summaryBatchesDirName)
    Utils.requireIsDirOrCanMakeDir(summaryBatchesDir)

    // write summaries to the file as nmt infer input file
    import scala.collection.JavaConverters.asJavaCollection
    // use `sliding` to produce summary batches
    for ((summaryBatch, index) <- summaries.sliding(batchSize, batchSize).zipWithIndex) {
      // create a directory for the batch
      val summaryBatchSaveDir = summaryBatchesDir.resolve(index.toString)
      Utils.requireIsDirOrCanMakeDir(summaryBatchSaveDir)
      // write the batch
      val summaryFile = summaryBatchSaveDir.resolve(s"$summaryFileName.txt")
      GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(summaryBatch)
    }

    // write categories to the file
    val categoryFile = saveDir.resolve(s"$categoryFileName.txt")
    GuavaFiles.asCharSink(categoryFile.toFile, Charsets.UTF_8) writeLines asJavaCollection(categories)
  }

  @throws[IOException]
  private def saveProductSummaryAndAmount(saveDir: Path,
                                          summaryFileName: String,
                                          amountFileName: String,
                                          sizeLimit: Int,
                                          summaryTransFunc: String => String = { s => s })
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(summaryFileName))
    require(!Strings.isNullOrEmpty(amountFileName))

    val tuples: Vector[(String, Double)] = SqlUtils.getProductSummaryAndAmount("product_summary", "vul_amount", sizeLimit)

    val summaries = tuples map { _._1 }
    val amounts = tuples map { _._2 }

    // write summaries to the file as nmt infer input file
    import scala.collection.JavaConverters.asJavaIterable
    val summaryFile: Path = saveDir.resolve(s"$summaryFileName.txt")
    GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

    // write amounts to the file
    val amountFile: Path = saveDir.resolve(s"$amountFileName.txt")
    GuavaFiles.asCharSink(amountFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(amounts map { _.toString })
  }

  @throws[IOException]
  private def saveProductSummaryAndImpact(saveDir: Path,
                                          summaryFileName: String,
                                          impactFileName: String,
                                          sizeLimit: Int,
                                          summaryTransFunc: String => String = { s => s })
  : Unit = {
    Utils.requireIsDirOrCanMakeDir(saveDir)
    require(!Strings.isNullOrEmpty(summaryFileName))
    require(!Strings.isNullOrEmpty(impactFileName))

    val tuples: Vector[(String, Double)] = SqlUtils.getProductSummaryAndImpact("product_summary", "vul_impact", sizeLimit)
    val summaries = tuples map { _._1 }
    val impact = tuples map { _._2 }

    // write summaries to the file as nmt infer input file
    import scala.collection.JavaConverters.asJavaIterable
    val summaryFile: Path = saveDir.resolve(s"$summaryFileName.txt")
    GuavaFiles.asCharSink(summaryFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(summaries map summaryTransFunc)

    // write impact to the file
    val impactFile: Path = saveDir.resolve(s"$impactFileName.txt")
    GuavaFiles.asCharSink(impactFile.toFile, Charsets.UTF_8) writeLines asJavaIterable(impact map { _.toString })
  }
}
