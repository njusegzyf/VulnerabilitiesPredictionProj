package nju.seg.zyf.vp.app

import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.IWekaMidResult

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp2GenerateMiddleResultRecordApp extends App {

  IWekaMidResult.convertMiddleResultFilesToRecords(Exp2Configs.exp2PathConfig.wekaMiddleResultDir,
                                                   Exp2Configs.exp2PathConfig.wekaMiddleResultRecordDir)
}
