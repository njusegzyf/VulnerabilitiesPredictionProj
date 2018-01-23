[String]$pythonLibPackagesDir = 'C:/Program Files/Python35/Lib/site-packages'

[String]$baseDir = 'E:/RNN'
[String]$sourceCodeDir = "$baseDir/NmtEncoderProj"
[String]$dataDir = "$baseDir/DataDir"
[String]$expDataDir = "$baseDir/Exp1DataDir"
[String]$trainDir = "$baseDir/TrainDir"

[String[]]$expNames = @('Exp1_Category')

# set console encoding to UTF-8
# [System.Console]::OutputEncoding=[System.Text.Encoding]::GetEncoding(65001)

# infer
Set-Location $sourceCodeDir
foreach ($expName in $expNames) {
  [String]$dataSummaryPostfix = "_Data_Summary"
  [String]$dataSummaryBatchFileName = "$expName$dataSummaryPostfix.txt"
  [String]$dataSummaryBatchesDir =  "$expDataDir/$expName$($dataSummaryPostfix)_Dir"

  if (Test-Path -Path $dataSummaryBatchesDir) {
    for ($i = 0; $i -lt 1024; $i = $i + 1) {
        $dataSummaryBatchDir = "$dataSummaryBatchesDir/$i"
        $dataSummaryBatchFile = "$dataSummaryBatchDir/$dataSummaryBatchFileName"

        if (-not ((Test-Path -Path $dataSummaryBatchDir) -and (Test-Path -Path $dataSummaryBatchFile))) {
          # if a batch do not exist, break
          break;
        }

        Write-Host "Start processing file: $dataSummaryBatchFile , at : $(Get-Date)."

        [String]$errOuputFile = "$dataSummaryBatchDir/$expName$($dataSummaryPostfix)_Infer_Err.txt"
        Out-File -FilePath $errOuputFile -Encoding utf8 
        #--src_max_len=$modelSrcAndTgtMaxLen  #--tgt_max_len=$modelSrcAndTgtMaxLen 
        python -m nmt.nmt `
            --model_dir=$trainDir `
            --out_dir=$trainDir `
            --vocab_prefix="$dataDir/vocab" `
            --train_prefix="$dataDir/train" `
            --dev_prefix="$dataDir/tst2012" `
            --test_prefix="$dataDir/tst2013" `
            --inference_input_file="$dataSummaryBatchFile" `
            --inference_output_file="$dataSummaryBatchDir/$expName$($dataSummaryPostfix)_Infer.txt" `
            2> $errOuputFile
    }
  }
}
