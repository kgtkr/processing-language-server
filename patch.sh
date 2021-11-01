#!/bin/sh -eu

COMMIT_ID=e6c1c0c45a02baf744cf02522eb83eb3b6ccc542
TGZ_CACHE_DIR=cache/processing4-src.tgz
CACHE_DIR=cache/processing4-src

mkdir -p $TGZ_CACHE_DIR

if [ ! -f $TGZ_CACHE_DIR/$COMMIT_ID ]; then
    wget https://github.com/processing/processing4/archive/$COMMIT_ID.tar.gz -O $TGZ_CACHE_DIR/$COMMIT_ID
fi

mkdir -p $CACHE_DIR

if [ ! -e $CACHE_DIR/$COMMIT_ID ]; then
    tar -xzf $TGZ_CACHE_DIR/$COMMIT_ID -C $CACHE_DIR
    mv $CACHE_DIR/processing4-$COMMIT_ID $CACHE_DIR/$COMMIT_ID
fi

resolve_patch () {
    MODE=$1
    SRC=$2
    PATCHED=$3
    PATCH=$4

    SRC_PATH=$CACHE_DIR/$COMMIT_ID/$SRC
    PATCHED_PATH=src/main/java/$PATCHED
    PATCH_PATH=patches/$PATCH
    PATCHED_DIR=$(dirname $PATCHED_PATH)

    if [ $MODE = "create" ]; then
        diff -up $SRC_PATH $PATCHED_PATH > $PATCH_PATH
    fi

    if [ $MODE = "apply" ]; then
        mkdir -p $PATCHED_DIR
        patch -u $SRC_PATH -i $PATCH_PATH -o $PATCHED_PATH
    fi
}


MODE=$1

if [ $MODE = "apply" ]; then
    rm -rf src/main/java
    mkdir -p src/main/java
fi


resolve_patch $MODE java/src/processing/mode/java/ErrorChecker.java processing/mode/java/ErrorChecker2.java ErrorChecker.patch
resolve_patch $MODE java/src/processing/mode/java/PreprocService.java processing/mode/java/PreprocService2.java PreprocService.patch
