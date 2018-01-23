package nju.seg.zyf.vp.app

import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.IWekaMidResult

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp3GenerateMiddleResultRecordApp extends App {

  IWekaMidResult.convertMiddleResultFilesToRecords(Exp3Configs.exp3PathConfig.wekaMiddleResultDir,
                                                   Exp3Configs.exp3PathConfig.wekaMiddleResultRecordDir)
}
