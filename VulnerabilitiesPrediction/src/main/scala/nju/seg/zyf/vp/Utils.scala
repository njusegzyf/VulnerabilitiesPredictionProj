package nju.seg.zyf.vp

import scala.util.Random
import scala.util.matching.Regex

import java.nio.file.{ Files, Path }
import javax.annotation.{ CheckReturnValue, Nonnull, ParametersAreNonnullByDefault }

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Utils {

  final case class PathConfig(trainDataDir: Path,
                              modelDir: Path,
                              evaluationDir: Path,
                              wekaMiddleResultDir: Path,
                              wekaMiddleResultRecordDir: Path) {

    this.allFolders foreach Utils.requireIsDirOrCanMakeDir

    @Nonnull @CheckReturnValue
    def allFolders: Iterable[Path] = Iterable(this.trainDataDir, this.modelDir, this.evaluationDir, this.wekaMiddleResultDir, this.wekaMiddleResultRecordDir)

    //    private def createDirectories(): Unit =
    //      this.allFolders foreach { Files.createDirectories(_) } // for (path <- this.allFolders) { Files.createDirectories(path) }
    //
    //    @throws[IllegalArgumentException]("if some folder can not be created or viewed")
    //    def createAndCheckDirectories(): Unit = {
    //      this.createDirectories()
    //      this.allFolders foreach { folder => if (!Files.isDirectory(folder)) throw new IllegalArgumentException }
    //      //      for (path <- this.allFolders) {
    //      //        if (!Files.isDirectory(path)) throw new IllegalArgumentException
    //      //      }
    //    }
  }

  def requireIsDirOrCanMakeDir(dir: Path): Unit = {
    require(dir != null)
    require(Files.isDirectory(Files.createDirectories(dir)))
  }

  @Nonnull
  val endLineCharRegex: Regex = """[\r\n]""".r

  @Nonnull @CheckReturnValue
  def removeEndLineChars(s: String): String = {
    require(s != null)

    endLineCharRegex.replaceAllIn(s, "")
  }

  @Nonnull
  val whitespaceRegex: Regex = """\s""".r

  @Nonnull @CheckReturnValue
  def whitespaceToUnderscore(s: String): String = {
    require(s != null)

    whitespaceRegex.replaceAllIn(s, "_")
  }

  @Nonnull @CheckReturnValue
  def selectProducts(products: Vector[String], sizeLimit: Int, randomSeed: Long): Vector[String] = {
    require(products != null)
    require(sizeLimit > 0)

    if (products.size <= sizeLimit) {
      return products
    }

    val productsSize: Int = products.size
    val random: Random = new Random(randomSeed)
    val startIndex = random.nextInt(productsSize)
    val selectIndexes: Iterable[Int] = Iterable.iterate(startIndex, sizeLimit) { _ => random.nextInt(productsSize) }
    val sortedDistinctIndexes: Vector[Int] = selectIndexes.toVector.distinct.sorted
    sortedDistinctIndexes map { products(_) }
  }
}
