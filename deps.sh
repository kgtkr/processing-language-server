#!/bin/sh -eu

VERSION=4.0b2
TGZ_CACHE_DIR=cache/processing.tgz
CACHE_DIR=cache/processing

mkdir -p $TGZ_CACHE_DIR

if [ ! -f $TGZ_CACHE_DIR/$VERSION ]; then
    wget https://github.com/processing/processing4/releases/download/processing-1277-4.0b2/processing-$VERSION-linux64.tgz -O $TGZ_CACHE_DIR/$VERSION
fi

mkdir -p $CACHE_DIR

if [ ! -e $CACHE_DIR/$VERSION ]; then
    tar -xzf $TGZ_CACHE_DIR/$VERSION -C $CACHE_DIR
    mv $CACHE_DIR/processing-$VERSION $CACHE_DIR/$VERSION
fi

rm -rf lib
mkdir lib
cp $CACHE_DIR/$VERSION/lib/*.jar $CACHE_DIR/$VERSION/core/library/*.jar $CACHE_DIR/$VERSION/modes/java/mode/*.jar lib
