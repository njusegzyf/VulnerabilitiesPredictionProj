package nju.seg.zyf.vp.app

import java.nio.file.Paths
import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.Utils
import nju.seg.zyf.vp.Utils.PathConfig
import weka.classifiers.AbstractClassifier
import weka.classifiers.trees.RandomForest

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
private[app] object Exp2Configs {

  val exp2Prefix: String = "Exp2"
  val exp2CategoryPrefix: String = s"${ Exp2Configs.exp2Prefix }_Category"
  val exp2AmountPrefix: String = s"${ Exp2Configs.exp2Prefix }_Amount"
  val exp2ImpactPrefix: String = s"${ Exp2Configs.exp2Prefix }_Impact"

  val exp2PathConfig: PathConfig = ExpConfigs.createPathConfig(ExpConfigs.baseDir.resolve("Exp2DataDir"))

  val exp2OneKeys: Vector[String] = Vector("attacker",
                                           "arbitrary",
                                           "vulnerability",
                                           "cve",
                                           "unspecified",
                                           "denial",
                                           "crafted",
                                           "cross")

  val exp2TwoKeys: Vector[String] = Vector("cve cross",
                                           "cve vulnerability",
                                           "cross vulnerability",
                                           "cve denial",
                                           "cross denial",
                                           "cve crafted",
                                           "cross crafted",
                                           "vulnerability denial")

  val exp2ThreeKeys: Vector[String] = Vector("cve cross crafted",
                                             "cve cross vulnerability",
                                             "cve cross denial",
                                             "cross crafted vulnerability",
                                             "cross crafted denial",
                                             "cross denial vulnerability",
                                             "cve crafted vulnerability",
                                             "cve crafted denial")

  val exp2AllKeys: Vector[String] = Vector(Exp2Configs.exp2OneKeys, Exp2Configs.exp2TwoKeys, Exp2Configs.exp2ThreeKeys).flatten

  val exp2AllKeyCombinations: Vector[String] = this.exp2AllKeys map Utils.whitespaceToUnderscore

  val exp2RnnLayer: Int = ExpConfigs.defaultExpRnnLayer
  val exp2RnnUnitNum: Int = ExpConfigs.defaultExpRnnUnitNum

  val exp2Algorithm: () => AbstractClassifier = () => new RandomForest

  val exp2DataInstancesSizeLimit: Int = 2000

  val exp2SkipWhenFileExisted: Boolean = !ExpConfigs.isInTest
}
