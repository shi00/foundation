#! /bin/bash
ROCKSDB_VER=$1
ROCKSDB_DIR=rocksdb-"$ROCKSDB_VER"
OUTPUT_SRC_DIR=$2
ROCKSDB_PKG=v"$ROCKSDB_VER".tar.gz
SHARDED_LIB_DIR=$3
SHARDED_LIB_NAME="librocksdb.so"

source /etc/profile

echo "================== Start downloading $ROCKSDB_PKG =================="
mkdir -p "$SHARDED_LIB_DIR"
wget -c -t 3 --no-check-certificate https://github.com/facebook/rocksdb/archive/refs/tags/"$ROCKSDB_PKG"
if [ $? -ne 0 ]; then
  echo "Failed to download $ROCKSDB_PKG"
  exit 1
fi

echo "Successfully downloaded $ROCKSDB_PKG"
tar zxvf "$ROCKSDB_PKG"
cd "$ROCKSDB_DIR"
make clean && make shared_lib
ldd "$SHARDED_LIB_NAME" | grep "not found"
if [ $? -ne 0 ]; then
  echo "Successfully built $SHARDED_LIB_NAME"
else
  ldd "$SHARDED_LIB_NAME"
  echo "Failed to built $SHARDED_LIB_NAME, some libraries $SHARDED_LIB_NAME depends on do not exist."
  exit 1
fi

mv "$SHARDED_LIB_NAME"."$ROCKSDB_VER" /opt/"$SHARDED_LIB_DIR"/"$SHARDED_LIB_NAME"
jextract --source --header-class-name RocksDB --output /opt/"$OUTPUT_SRC_DIR" -t com.silong.foundation.rocksdbffm -I /opt/"$ROCKSDB_DIR"/include/rocksdb -I /opt/"$ROCKSDB_DIR"/include/rocksdb/utilities /opt/"$ROCKSDB_DIR"/include/rocksdb/c.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 10s