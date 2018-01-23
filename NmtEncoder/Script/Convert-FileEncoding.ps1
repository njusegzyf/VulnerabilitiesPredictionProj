param($sourcePath)
param($destPath)

if (Test-Path $sourcePath -PathType Container) {
    if (-not(Test-Path $destPath)){
      New-Item -Path $destPath -ItemType Directory 
    }

    $children = Get-ChildItem $sourcePath

    foreach ($child in $children) {
      Get-Content $child.FullName | Out-File "$destPath/$($child.Name)" -Encoding utf8
    }
}
