#!/bin/bash

BINDER_DIR=$1
OPTIMIZATIONS_DIR=$2
OPTIMIZATION_UUID=$3
SLUCAJNI=`cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1`

# Kreira se fajl sa slucajnim nazivom. Broj tih fajlova upucuje na broj trenutno aktivnih evaluacija 
STATS_FILE=${BINDER_DIR}/${OPTIMIZATIONS_DIR}/${OPTIMIZATION_UUID}/.stats-${SLUCAJNI}

touch ${STATS_FILE}

# U nastavku je dummy ponasanje
read -r BROJ
read -r A
read -r B

sleep 20

echo OK
echo 2
echo "$A+$B" | bc
echo "$A*$B" | bc

# Obrisi fajl kad se zavrsi evaluacija
rm ${STATS_FILE}

