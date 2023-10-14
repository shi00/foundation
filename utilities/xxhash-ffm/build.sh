#! /bin/sh
XXHASH_VER=$1
XXHASH_DIR=xxHash-"$XXHASH_VER"
OUTPUT_SRC_DIR=$2
XXHASH_PKG=v"$XXHASH_VER".tar.gz
SHARDED_LIB_DIR=$3
OS_NAME=$4
OS_ARCH=$5
SHARDED_LIB_NAME="libxxhash.so"

source /etc/profile

echo "$OS_NAME" "$OS_ARCH" "$XXHASH_VER" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR"

echo "================== Start downloading $XXHASH_PKG =================="
mkdir -p "$SHARDED_LIB_DIR"
wget -c -t 3 --no-check-certificate https://github.com/Cyan4973/xxHash/archive/refs/tags/"$XXHASH_PKG"
if [ $? -ne 0 ]; then
  echo "Failed to download $XXHASH_PKG"
  exit 1
fi

echo "Successfully downloaded $XXHASH_PKG"
tar zxvf "$XXHASH_PKG"
cd "$XXHASH_DIR"
make clean && make
if [ ! -f "$SHARDED_LIB_NAME"."$XXHASH_VER" ];then
  echo "Failed to build $SHARDED_LIB_NAME.$XXHASH_VER"
  exit 1
fi

ldd "$SHARDED_LIB_NAME" | grep "not found"
if [ $? -ne 0 ]; then
  echo "Successfully built $SHARDED_LIB_NAME"
else
  ldd "$SHARDED_LIB_NAME"
  echo "Failed to built $SHARDED_LIB_NAME, some libraries $SHARDED_LIB_NAME depends on do not exist."
  exit 1
fi

mkdir -p /opt/"$SHARDED_LIB_DIR"/"$OS_NAME"/"$OS_ARCH"
mv "$SHARDED_LIB_NAME"."$XXHASH_VER" /opt/"$SHARDED_LIB_DIR"/"$OS_NAME"/"$OS_ARCH"/"$SHARDED_LIB_NAME"
jextract --source --header-class-name Xxhash --output /opt/"$OUTPUT_SRC_DIR" -t com.silong.foundation.utilities.xxhash.generated -I /opt/"$XXHASH_DIR" /opt/"$XXHASH_DIR"/xxhash.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 10s