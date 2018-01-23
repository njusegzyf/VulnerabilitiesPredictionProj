New-Variable -Name 'pythonLibPackagesDir' -Value 'C:/Program Files/Python35/Lib/site-packages' -Scope 'Global'

New-Variable -Name 'vpProjBaseDir' -Value 'C:/IDEAProjs/VulnerabilitiesPredictionProj' -Scope 'Global'

New-Variable -Name 'vpDemoBaseDir' -Value "$vpProjBaseDir/VulnerabilitiesPredictionDemo" -Scope 'Global'
New-Variable -Name 'vpDemoScriptDir' -Value "$vpDemoBaseDir/srcmain/resources" -Scope 'Global'
New-Variable -Name 'vpDemoDataDir' -Value "$vpDemoBaseDir/data/DataDir" -Scope 'Global'
New-Variable -Name 'vpDemoTrainDir' -Value "$vpDemoBaseDir/data/TrainDir" -Scope 'Global'

New-Variable -Name 'vpNmtEncoderProjDir' -Value "$vpProjBaseDir/NmtEncoder" -Scope 'Global'

New-Variable -Name 'vpDemoSkipWhenOutputFileExists' -Value $false -Scope 'Global'
