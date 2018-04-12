#!/bin/sh
set -euo 
#pipefail

. instance
mkdir -p uploads
mkdir -p archive
VERFILE=src/main/resources/buildno.txt
VERSION=`head -1 $VERFILE`
ZIPFILE=`pwd`/uploads/presheaf2.zip
ZIPNAME=${ZIPFILE##*/}

UP="$SCP uploads/*"
echo "Have file $ZIPFILE, will $UP"
#GETLOGS="$SCP $HOMETHERE/tomcat/logs/* logs/"
echo `date`> "uploads/ready2.flag"
mv -f $ZIPFILE archive/$ZIPNAME.$VERSION || true
pushd target/universal/stage
zip -r $ZIPFILE ./*
popd
echo "got version `cat src/main/resources/buildno.txt`"
#echo "presheaf-06062011-256MlU04JcS1o"
$SCP uploads/*.zip
$SCP uploads/ready2.flag
echo "Uploaded $ZIPFILE, `date`" 
#sleep 600 
#$GETLOGS

