package nju.seg.zyf.vp.demo

import java.nio.file.{ Files, Path }
import javax.annotation.ParametersAreNonnullByDefault

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[vp] object Demo2Config extends IDemoConfig {

  override def demoName: String = "Demo2"

  override val outputPath: Path = super.outputPath

  this.init()

  private[this] def init(): Unit = {

    val outputPath: Path = this.outputPath

    if (Files.isRegularFile(outputPath)) {
      throw new IllegalArgumentException
    }

    if (!Files.isDirectory(this.outputPath)) {
      Files.createDirectories(this.outputPath)
    }
  }

}
