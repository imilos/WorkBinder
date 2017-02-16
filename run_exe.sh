#!/bin/bash

ULAZNI_FAJL=$1

# Brise NULL karaktere sa kraja ulaznog fajla
#tr -d '\000' < $1 > $ULAZNI_FAJL

# Brisi NULL karaktere u csv tabelama
#for FAJL in *.csv ; do tr -d '\000' < $FAJL > $FAJL-1; mv $FAJL-1 $FAJL; done

# Prosledjuje se fajl sa ponderima kao argument komandne linije
#Rscript Script.R-uploaded $ULAZNI_FAJL 2>/dev/null

# Kreira se fajl sa slucajnim nazivom. Broj tih fajlova upucuje na broj trenutno
# aktivnih evaluacija 
SLUCAJNI=`cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1`
touch stats-${SLUCAJNI}

# U nastavku je dummy ponasanje
read -r BROJ
read -r A
read -r B

echo SUCCESS
echo 2
echo "$A+$B" | bc
echo "$A*$B" | bc

# Obrisi fajl kad se zavrsi evaluacija
#rm -f stats-${SLUCAJNI}
