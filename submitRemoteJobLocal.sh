#!/bin/bash

# Script for submitting server jobs on the remote CE
# Creates a temporary jdl file and submits it

CE=$1
ROUTINGINFO=$2
JOBID=$3
LOGFILE=$4

#echo ""
#echo ""
echo "SUBMITTED: $CE $ROUTINGINFO $JOBID $LOGFILE"

sh startBinderJobLocal.sh $1 $2 $3 $4

if [ ! $? = 0 ]; then
    echo "Error submitting remote job."
    exit 1
else 
    echo "Job successfully submitted!"
fi


