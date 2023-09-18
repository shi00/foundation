#! /bin/bash
ROCKSDB_VER=$1
ROCKSDB_DIR=rocksdb-"$ROCKSDB_VER"
OUTPUT_SRC_DIR=$2
ROCKSDB_PKG=v"$ROCKSDB_VER".tar.gz
SHARDED_LIB_DIR=$3

source /etc/profile

echo "==================Start downloading $ROCKSDB_PKG=================="
mkdir -p "$SHARDED_LIB_DIR" \
&& wget -c -t 3 --no-check-certificate https://github.com/facebook/rocksdb/archive/refs/tags/"$ROCKSDB_PKG" \
&& tar zxvf "$ROCKSDB_PKG" && cd "$ROCKSDB_DIR" \
&& make clean && make shared_lib \
&& ldd librocksdb.so \
&& mv librocksdb.so."$ROCKSDB_VER" /opt/"$SHARDED_LIB_DIR"/librocksdb.so \
&& jextract --source --header-class-name RocksDB --output /opt/"$OUTPUT_SRC_DIR" -t com.silong.foundation.rocksdbffm -I /opt/"$ROCKSDB_DIR"/include/rocksdb -I /opt/"$ROCKSDB_DIR"/include/rocksdb/utilities /opt/"$ROCKSDB_DIR"/include/rocksdb/c.h
#cd .. && mkdir -p output && mv "$OUTPUT_SRC_DIR" output && mv "$ROCKSDB_DIR"/librocksdb.so."$ROCKSDB_VER" output/librocksdb.so
echo "==================Build completed=================="
