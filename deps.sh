#!/bin/sh -eu
CACHE_DIR=cache

BIN_TGZ_CACHE_DIR=$CACHE_DIR/processing-tgz
mkdir -p $BIN_TGZ_CACHE_DIR
if [ ! -e $BIN_TGZ_CACHE_DIR/$TAG ]; then
    BIN_URL=https://github.com/processing/$REPO/releases/download/$TAG/$ASSET_NAME
    wget $BIN_URL -O $BIN_TGZ_CACHE_DIR/$TAG
fi

BIN_CACHE_DIR=$CACHE_DIR/processing
mkdir -p $BIN_CACHE_DIR
if [ ! -e $BIN_CACHE_DIR/$TAG ]; then
    tar -xzf $BIN_TGZ_CACHE_DIR/$TAG -C $BIN_CACHE_DIR
    mv $BIN_CACHE_DIR/processing-$VERSION $BIN_CACHE_DIR/$TAG
fi

rm -rf lib
mkdir lib
cp $BIN_CACHE_DIR/$TAG/lib/*.jar $BIN_CACHE_DIR/$TAG/core/library/*.jar $BIN_CACHE_DIR/$TAG/modes/java/mode/*.jar lib

SRC_TGZ_CACHE_DIR=$CACHE_DIR/processing-src-tgz
mkdir -p $SRC_TGZ_CACHE_DIR
if [ ! -e $SRC_TGZ_CACHE_DIR/$TAG ]; then
    SRC_URL=https://github.com/processing/$REPO/archive/refs/tags/$TAG.tar.gz
    wget $SRC_URL -O $SRC_TGZ_CACHE_DIR/$TAG
fi

SRC_CACHE_DIR=$CACHE_DIR/processing-src
mkdir -p $SRC_CACHE_DIR
if [ ! -e $SRC_CACHE_DIR/$TAG ]; then
    tar -xzf $SRC_TGZ_CACHE_DIR/$TAG -C $SRC_CACHE_DIR
    mv $SRC_CACHE_DIR/$REPO-$TAG $SRC_CACHE_DIR/$TAG
fi

