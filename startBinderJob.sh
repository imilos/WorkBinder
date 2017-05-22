#!/bin/bash

# Script for starting server jobs on the remote CE
# Each job will be restarted 50 times

echo CE = $1
echo routingInfo = $2
echo jobID = $3
echo LogFilename = $4

# Zapamti tekuci direktorijum
BINDER_DIR=${PWD}

# Napravi privremeni dir za datu instancu WorkerDispatcher-a
TMP_DIR=`mktemp -d -t --tmpdir=./tmp/`

# Iskopiraj sve optimizacije u $TMP_DIR kao hard links
cp -r ${BINDER_DIR}/optimizacije ${TMP_DIR}/optimizacije

# Promena moda za svaki slucaj
chmod -R +x ${TMP_DIR}/optimizacije

# Predji u privremeni direktorijum
cd ${TMP_DIR}

# Napravi properties fajl sa maticnom lokacijom BINDER_DIR
echo "BinderDir=${BINDER_DIR}" > Worker.properties

MAX_WALL_CLOCK_TIME=1000

count=0
while [ $count -lt 50 ]
do
  count=`expr $count + 1`
  #java -cp "bin/:lib/log4j-1.2.15.jar" yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "WorkerDispatcher.properties" $1 $2 $3 $4 $MAX_WALL_CLOCK_TIME
  java -cp "${BINDER_DIR}/bin/:${BINDER_DIR}/lib/log4j-1.2.15.jar" yu.ac.bg.rcub.binder.handler.worker.WorkerDispatcher "${BINDER_DIR}/WorkerDispatcher.properties" $1 $2 $3 $4 $MAX_WALL_CLOCK_TIME
done

# Obrisi ceo temporary direktorijum
rm -rf ${TMP_DIR}

echo Server Job finished.

