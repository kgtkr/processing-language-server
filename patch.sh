#!/bin/sh -eu
CACHE_DIR=cache
SRC_CACHE_DIR=$CACHE_DIR/processing-src

resolve_patch () {
    MODE=$1
    SRC=$2
    PATCHED=$3
    PATCH=$4

    SRC_PATH=$SRC_CACHE_DIR/$TAG/$SRC
    PATCHED_PATH=src/main/java/$PATCHED
    PATCH_PATH=patches/$PATCH_NAME/$PATCH
    PATCHED_DIR=$(dirname $PATCHED_PATH)

    if [ $MODE = "create" ]; then
        set +e
        diff -up $SRC_PATH $PATCHED_PATH > $PATCH_PATH
        diff_code=$?
        if [ $diff_code -ne 0 ] && [ $diff_code -ne 1 ]; then
            echo "Failed to create patch $PATCH_PATH"
            exit 1
        fi
        set -e
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
