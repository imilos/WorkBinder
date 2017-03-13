#!/bin/bash

echo "***********************"
echo "ClenUp start"
echo "***********************"

# echo $1
# echo $2
# echo $3
# echo $4

dirOptimisations=$1
prefiks=$2"*";
pathLogFile=$3
dirDispatcherLog=$4 

# echo $dirOptimisations
# echo $prefiks
# echo $pathLogFile
# echo $dirDispatcherLog;
# -type f 

echo "Brisanje fajlova sa prefiksom " $prefiks 
find $dirOptimisations -name $prefiks -delete
echo "Brisanje log file: " $pathLogFile
rm $pathLogFile

echo "Brisanje sadrzaja foldera" $dirDispatcherLog
rm -r $dirDispatcherLog/*

echo "***********************"
echo "CleanUp end"
echo "***********************"
