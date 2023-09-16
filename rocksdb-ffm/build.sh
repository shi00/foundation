#! /bin/bash
ROCKSDB_VER=$1
ROCKSDB_DIR=rocksdb-"$ROCKSDB_VER"
OUTPUT_DIR=$2
ROCKSDB_PKG=v"$ROCKSDB_VER".tar.gz

echo "Start downloading $ROCKSDB_PKG ......"

wget -c -t 3 --no-check-certificate https://github.com/facebook/rocksdb/archive/refs/tags/"$ROCKSDB_PKG"
tar zxvf "$ROCKSDB_PKG" && cd "$ROCKSDB_DIR"
source /etc/profile
make clean && make shared_lib
ldd librocksdb.so
jextract --source --header-class-name RocksDB --output /opt/"$OUTPUT_DIR" -t com.silong.foundation.rocksdbffm -I /opt/"$ROCKSDB_DIR"/include/rocksdb -I /opt/"$ROCKSDB_DIR"/include/rocksdb/utilities /opt/"$ROCKSDB_DIR"/include/rocksdb/c.h

