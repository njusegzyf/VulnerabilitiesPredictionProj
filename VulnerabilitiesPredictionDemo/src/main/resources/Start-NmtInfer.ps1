Param ([String]$vpDemoName)

# Set-ExecutionPolicy UnRestricted

# import settings
."C:/IDEAProjs/VulnerabilitiesPredictionProj/VulnerabilitiesPredictionDemo/src/main/resources/Config.ps1"

[String]$vpDemoOutputDir = "$vpDemoBaseDir/data/$vpDemoName"

# set console encoding to UTF-8
# [System.Console]::OutputEncoding=[System.Text.Encoding]::GetEncoding(65001)

[String]$dataSummaryFile = "$vpDemoOutputDir/$($vpDemoName)_Data_Summary.txt"

if (-not (Test-Path -Path $dataSummaryFile)) {
    return $false;
}

[String]$outputFile ="$vpDemoOutputDir/$($vpDemoName)_Infer.txt"
# Note: We put output of encoder in the "errOuputFile".
[String]$errOuputFile = "$vpDemoOutputDir/$($vpDemoName)_Infer_Err.txt"
[String]$errOuputFileUTF8 = "$vpDemoOutputDir/$($vpDemoName)_Infer_Err_UTF8.txt"

# skip if 'skipWhenOutputFileExists' is true and the output file exists
if ($vpDemoSkipWhenOutputFileExists -and (Test-Path -Path $outputFile)) {
    Write-Host "Skip processing file: $dataSummaryFile as it already eists."
    return $true;
}

Write-Host "Start processing file: $dataSummaryFile , at : $(Get-Date)."

Set-Location $vpNmtEncoderProjDir

python -m nmt.nmt `
  --model_dir=$vpDemoTrainDir `
  --out_dir=$vpDemoTrainDir `
  --vocab_prefix="$vpDemoDataDir/vocab" `
  --train_prefix="$vpDemoDataDir/train" `
  --dev_prefix="$vpDemoDataDir/tst2012" `
  --test_prefix="$vpDemoDataDir/tst2013" `
  --inference_input_file=$dataSummaryFile `
  --inference_output_file=$outputFile `
  2> $errOuputFile

# create a copy of err output  file encoded with UTF8
Get-Content $errOuputFile | Out-File $errOuputFileUTF8 -Encoding UTF8
