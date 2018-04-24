#!/bin/bash
# Copy me to ~, create a ~/cache folder
# This script is for testing presheaf locally, while the remote one works ok

set -e
rm -f out.tmp
cache=/Users/vpatryshev/cache
id=$1
src=$id.src
pdf=$id.pdf
png=$id.png
echo "Rendering $id.src -> $id.pdf and $id.png"
curl -G --data-urlencode format=xy --data-urlencode in@$cache/$src http://presheaf.com/dws > $id.out
hash=`cat $id.out | sed 's/.*\"id\":\"//' | sed 's/".*//'`
rm $id.out

curl "http://presheaf.com/cache/$hash.pdf" > $cache/$pdf
curl "http://presheaf.com/cache/$hash.png" > $cache/$png

