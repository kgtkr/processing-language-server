#!/bin/sh -eu

BASENAME=processing-4.0b2
NAME=$BASENAME-linux64
URL=https://github.com/processing/processing4/releases/download/processing-1277-4.0b2/$NAME.tgz

if [ -e cache/$NAME.tgz ]; then
    exit
fi

cd cache

wget -L $URL
tar -xzf $NAME.tgz

cd ../

rm -rf lib
mkdir lib
cp cache/$BASENAME/lib/*.jar cache/$BASENAME/core/library/*.jar cache/$BASENAME/modes/java/mode/*.jar lib

rm -rf cache/$BASENAME
