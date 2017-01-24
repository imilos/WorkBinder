#!/bin/bash

# Script for submitting server jobs on the remote CE
# Creates a temporary jdl file and submits it

CE=$1
ROUTINGINFO=$2
JOBID=$3
LOGFILE=$4

cat > tmp_remotejob_$JOBID.jdl <<EOF
 Executable="startBinderJob.sh";
 StdOutput="$LOGFILE-out";
 StdError="$LOGFILE-err"; 
 InputSandbox={"startBinderJob.sh"};
 OutputSandbox={"$LOGFILE-out", "$LOGFILE-err", "$LOGFILE.tar.gz"};
 Arguments="$1 $2 $3 $4";
 
 Requirements = \
other.GlueCEUniqueID == "$CE";
 ShallowRetryCount = 10;
EOF
   
# submit jdl to grid
#glite-wms-job-submit -e https://wms-aegis.phy.bg.ac.yu:7443/glite_wms_wmproxy_server -a -o jobIDs tmp_remotejob_$JOBID.jdl > /dev/null
#glite-wms-job-submit -a -o jobIDs tmp_remotejob_$JOBID.jdl > /dev/null
glite-wms-job-submit -a -o jobIDs tmp_remotejob_$JOBID.jdl > /dev/null

if [ ! $? = 0 ]; then
    echo "Error submitting remote job."
    exit 1
else 
    echo "Job successfully submitted!"
fi
  
# delete temp jdl file
rm -f tmp_remotejob_$JOBID.jdl

