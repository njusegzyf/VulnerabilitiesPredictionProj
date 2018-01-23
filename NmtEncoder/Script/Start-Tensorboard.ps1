$tensorFlowPath = 'C:/Program Files/Python35/Lib/site-packages/tensorflow'
$logDir = "E:/RNN/OutDir/SummaryDir"

python "$tensorFlowPath/tensorboard/tensorboard.py" --logdir=$logDir
