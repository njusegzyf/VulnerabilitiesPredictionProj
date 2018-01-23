param($outDir)

$SITE_PREFIX = "https://nlp.stanford.edu/projects/nmt/data"

if (-not (Test-Path -Path $outDir)) {
  New-Item -Path $outDir -ItemType Directory
}

function Download-FileIfNonExist($name) {
  $outPutPathEn = "$outDir/$name.en"
  $outPutPathVi = "$outDir/$name.vi"
  if (-not (Test-Path $outPutPathEn)) {
    Invoke-WebRequest "$SITE_PREFIX/iwslt15.en-vi/$name.en" -OutFile $outPutPathEn
  }
    if (-not (Test-Path $outPutPathVi)) {
    Invoke-WebRequest "$SITE_PREFIX/iwslt15.en-vi/$name.vi" -OutFile $outPutPathVi
  }
}

# Download iwslt15 small dataset from standford website.
Write-Host 'Download training dataset train.en and train.vi.'
Download-FileIfNonExist "train"

Write-Host "Download dev dataset tst2012.en and tst2012.vi."
Download-FileIfNonExist "tst2012"

Write-Host "Download test dataset tst2013.en and tst2013.vi."
Download-FileIfNonExist "tst2013"

Write-Host "Download vocab file vocab.en and vocab.vi."
Download-FileIfNonExist "vocab"
