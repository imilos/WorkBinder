#!/bin/bash

BINDER_DIR=$1
OPTIMIZATIONS_DIR=$2
OPTIMIZATION_UUID=$3
SLUCAJNI=`cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1`
XMLINPUT=$4

# Kreira se fajl sa slucajnim nazivom. Broj tih fajlova upucuje na broj trenutno aktivnih evaluacija 
STATS_FILE=${BINDER_DIR}/${OPTIMIZATIONS_DIR}/${OPTIMIZATION_UUID}/.stats-${SLUCAJNI}

touch ${STATS_FILE}

sleep 1

# Kopira ulazni XML u izlazni XML
cp ${XMLINPUT} output.xml

echo OK

# Obrisi fajl kad se zavrsi evaluacija
rm ${STATS_FILE}
