[String]$pythonLibPackagesDir = 'C:/Program Files/Python35/Lib/site-packages'

[String]$baseDir = 'E:/RNN'
[String]$sourceCodeDir = "$baseDir/NmtEncoderProj"
[String]$dataDir = "$baseDir/DataDir"
[String]$expDataDir = "$baseDir/Exp3DataDir/trainData"
[String]$trainDir = "$baseDir/TrainDir"

[Boolean]$skipWhenOutputFileExists = $false

[String[]]$expNames = @('Exp3_Category', 'Exp3_Amount', 'Exp3_Impact')

[String[]]$expSearchEngines = @("Bing", "Baidu", "Yahoo")

# set console encoding to UTF-8
# [System.Console]::OutputEncoding=[System.Text.Encoding]::GetEncoding(65001)

# infer
Set-Location $sourceCodeDir
foreach ($expName in $expNames) {
  foreach ($searchEngine in $expSearchEngines) {
      [String]$dataSummaryPostfix = "_$($searchEngine)_Data_Summary"
      [String]$dataSummaryFileName = "$expName$dataSummaryPostfix.txt"
      [String]$dataSummaryFile = "$expDataDir/$dataSummaryFileName"

      if (-not (Test-Path -Path $dataSummaryFile)) {
        continue;
      }

      Write-Host "Start processing file: $dataSummaryFileName , at : $(Get-Date)."

      [String]$errOuputFile = "$expDataDir/$expName$($dataSummaryPostfix)_Infer_Err.txt"
      [String]$errOuputFileUTF8 = "$expDataDir/$expName$($dataSummaryPostfix)_Infer_Err_UTF8.txt"
      if ($skipWhenOutputFileExists -and (Test-Path -Path $errOuputFile)) {
        continue;
      }

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
      # create a copy of err output  file encoded with UTF8
      Get-Content $errOuputFile | Out-File $errOuputFileUTF8 -Encoding UTF8
  }
}
