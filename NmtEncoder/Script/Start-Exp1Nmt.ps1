[String]$pythonLibPackagesDir = 'C:/Program Files/Python35/Lib/site-packages'

[String]$baseDir = 'E:/RNN'
[String]$sourceCodeDir = "$baseDir/NmtEncoderProj"
[String]$dataDir = "$baseDir/DataDir"
[String]$expDataDir = "$baseDir/Exp1DataDir/trainData"
[String]$trainDir = "$baseDir/TrainDir"

#[String]$modelSrcAndTgtMaxLen = (1024*1024*16).ToString()

[String[]]$expNames = @('Exp1_Category', 'Exp1_Amount', 'Exp1_Impact')

# set console encoding to UTF-8
# [System.Console]::OutputEncoding=[System.Text.Encoding]::GetEncoding(65001)

# infer
Set-Location $sourceCodeDir
foreach ($expName in $expNames) {
  [String]$dataSummaryPostfix = "_Data_Summary"
  [String]$dataSummaryFileName = "$expName$dataSummaryPostfix.txt"
  [String]$dataSummaryFile = "$expDataDir/$dataSummaryFileName"

  if (-not (Test-Path -Path $dataSummaryFile)) {
    continue;
  }

  Write-Host "Start processing file: $dataSummaryFileName , at : $(Get-Date)."

  [String]$errOuputFile = "$expDataDir/$expName$($dataSummaryPostfix)_Infer_Err_UTF8.txt"
  Out-File -FilePath $errOuputFile -Encoding utf8 
  #--src_max_len=$modelSrcAndTgtMaxLen  #--tgt_max_len=$modelSrcAndTgtMaxLen 
  python -m nmt.nmt `
    --model_dir=$trainDir `
    --out_dir=$trainDir `
    --vocab_prefix="$dataDir/vocab" `
    --train_prefix="$dataDir/train" `
    --dev_prefix="$dataDir/tst2012" `
    --test_prefix="$dataDir/tst2013" `
    --inference_input_file=$dataSummaryFile `
    --inference_output_file="$expDataDir/$expName$($dataSummaryPostfix)_Infer.txt" `
    2> $errOuputFile
}


