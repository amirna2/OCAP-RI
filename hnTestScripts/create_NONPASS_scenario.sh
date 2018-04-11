#! /bin/bash
# Create a scenario file of all the NONPASS tests

outfile=~/atelite/results/home_networking/NONPASS_`date "+%b%d%Y"`.scenario
echo $outfile

cd ~/atelite/results/home_networking
cat results.csv | grep NONPASS | sed s/,NONPASS// | sed s/ocap/tset.builder.ocap/ | sed s/,/\\t/g > $outfile


