#!/bin/sh -eu
CACHE_DIR=cache

BIN_ARC_CACHE_DIR=$CACHE_DIR/processing-arc
mkdir -p $BIN_ARC_CACHE_DIR
if [ ! -e $BIN_ARC_CACHE_DIR/$TAG ]; then
    BIN_URL=https://github.com/processing/$REPO/releases/download/$TAG/$ASSET_NAME
    wget $BIN_URL -O $BIN_ARC_CACHE_DIR/$TAG
fi

BIN_CACHE_DIR=$CACHE_DIR/processing
mkdir -p $BIN_CACHE_DIR
if [ ! -e $BIN_CACHE_DIR/$TAG ]; then
    if [ "$PROCESSING_ARC" = "zip" ]; then
        unzip -d $BIN_CACHE_DIR $BIN_ARC_CACHE_DIR/$TAG
    fi
    if [ "$PROCESSING_ARC" = "tgz" ]; then
        tar -xzf $BIN_ARC_CACHE_DIR/$TAG -C $BIN_CACHE_DIR
    fi
    if [ "$PROCESSING_OS" = "macos" ]; then
        mv $BIN_CACHE_DIR/Processing.app $BIN_CACHE_DIR/$TAG
    else
        mv $BIN_CACHE_DIR/processing-$VERSION $BIN_CACHE_DIR/$TAG
    fi
fi

rm -rf lib
mkdir lib
if [ "$PROCESSING_OS" = "macos" ]; then
    BIN_CACHE_JAR_DIR=$BIN_CACHE_DIR/$TAG/Contents/Java
else
    BIN_CACHE_JAR_DIR=$BIN_CACHE_DIR/$TAG
fi
cp $BIN_CACHE_JAR_DIR/lib/*.jar $BIN_CACHE_JAR_DIR/core/library/*.jar $BIN_CACHE_JAR_DIR/modes/java/mode/*.jar lib


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

