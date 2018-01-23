package nju.seg.zyf.vp.app

import javax.annotation.ParametersAreNonnullByDefault

import nju.seg.zyf.vp.IWekaMidResult

/**
  * @author Zhang Yifan
  */
@ParametersAreNonnullByDefault
object Exp4GenerateMiddleResultRecordApp extends App {

  IWekaMidResult.convertMiddleResultFilesToRecords(Exp4Configs.exp4PathConfig.wekaMiddleResultDir,
                                                   Exp4Configs.exp4PathConfig.wekaMiddleResultRecordDir)
}
