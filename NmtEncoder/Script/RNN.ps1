[String]$pythonLibPackagesDir = 'C:/Program Files/Python35/Lib/site-packages'

[String]$baseDir = 'E:/RNN'
[String]$scriptDir = "$baseDir/NmtEncoderProj/Script"
[String]$sourceCodeDir = "$baseDir/NmtEncoderProj"
[String]$dataDir = "$baseDir/DataDir"
[String]$trainDir = "$baseDir/TrainDir"
[String]$outputDir = "$baseDir/OutDir"

[String]$inferFileName = "tst2013.en"

[String]$modelSrcAndTgtMaxLen = (50).ToString()


# download data
# ."$scriptDir/Download-Iwslt15.ps1" -outDir $dataDir

# train model
if (-not (Test-Path -Path $trainDir)) {
  New-Item -Path $trainDir -ItemType Directory
}
Set-Location "$sourceCodeDir"
python -m nmt.nmt `
  --src=en --tgt=vi `
  --src_max_len=$modelSrcAndTgtMaxLen `
  --tgt_max_len=$modelSrcAndTgtMaxLen `
  --vocab_prefix="$dataDir/vocab" `
  --train_prefix="$dataDir/train" `
  --dev_prefix="$dataDir/tst2012" `
  --test_prefix="$dataDir/tst2013" `
  --out_dir=$trainDir `
  --num_train_steps=12000 `
  --steps_per_stats=100 `
  --num_layers=2 `
  --num_units=128 `
  --dropout=0.2 `
  --metrics=bleu



# infer
if (-not (Test-Path -Path $outputDir)) {
  New-Item -Path $outputDir -ItemType Directory
}
Set-Location "$sourceCodeDir"
# create an empty file encoded with UTF8, so that the stand error is output to the file encoded with UTF8 instead of GB2312
[String]$errOuputFile = "$outputDir/$($inferFileName)_err.txt"
Out-File -FilePath $errOuputFile -Encoding utf8 
python -m nmt.nmt `
  --src_max_len=$modelSrcAndTgtMaxLen `
  --tgt_max_len=$modelSrcAndTgtMaxLen `
  --model_dir=$trainDir `
  --out_dir=$trainDir `
  --vocab_prefix="$dataDir/vocab" `
  --train_prefix="$dataDir/train" `
  --dev_prefix="$dataDir/tst2012" `
  --test_prefix="$dataDir/tst2013" `
  --inference_input_file="$dataDir/$inferFileName" `
  --inference_output_file="$outputDir/$($inferFileName)_infer" `
  2> $errOuputFile
