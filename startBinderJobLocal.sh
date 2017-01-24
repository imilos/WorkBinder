#!/bin/bash

# Script for starting server jobs on the remote CE
# Each job will be restarted 50 times

echo CE = $1
echo routingInfo = $2
echo jobID = $3
echo Log Filename = $4

chmod a+x binder.jar external.jar MojExe.exe run_exe.sh

JOB_START_TIME=`expr \`date +'%s'\` / 60` # current time in minutes 
CE=`$EDG_LOCATION/bin/edg-brokerinfo getCE`
CE_GRID_HOST=`echo "$CE"| cut -d ":" -f 1`

# in minutes
MAX_WALL_CLOCK_TIME=`ldapsearch -x -h $CE_GRID_HOST -p 2135 -b "GlueCEUniqueID=$CE,mds-vo-name=local,o=grid" 'objectclass=GlueCEPolicy' GlueCEPolicyMaxWallClockTime | grep GlueCEPolicyMaxWallClockTime: | cut -d ' ' -f 2`
if [ -z $MAX_WALL_CLOCK_TIME ]; then
  MAX_WALL_CLOCK_TIME=3600
fi
echo MAX_WALL_CLOCK_TIME = $MAX_WALL_CLOCK_TIME

count=0
while [ $count -lt 50 -a $MAX_WALL_CLOCK_TIME -gt 0 ]
do
  count=`expr $count + 1`
  #java -cp "binder.jar:external.jar:log4j-1.2.15.jar" yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "WorkerDispatcher.properties" $1 $2 $3 $4_$count $MAX_WALL_CLOCK_TIME
  #java -cp "bin/:lib/log4j-1.2.15.jar" yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "WorkerDispatcher.properties" $1 $2 $3 $4_$count $MAX_WALL_CLOCK_TIME
  java -cp "bin/:lib/log4j-1.2.15.jar" yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "WorkerDispatcher.properties" $1 $2 $3 $4 $MAX_WALL_CLOCK_TIME
  NOW=`expr \`date +'%s'\` / 60` # current time in minutes
  MAX_WALL_CLOCK_TIME=`expr $MAX_WALL_CLOCK_TIME - $NOW + $JOB_START_TIME`
done

# compress all output log files into one file
#tar -cvzf $4.tar.gz ${4}_*

# delete log files for good manner
#rm -f ${4}_*

echo Server Job finished.

# java -cp "." yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "WorkerDispatcher.properties" $1 $2 $3 $4 finished
 
