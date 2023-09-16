#! /bin/sh
ROCKSDB_VER=$1
ROCKSDB_DIR=rocksdb-"$ROCKSDB_VER"
OUTPUT_DIR=$2

cd /opt \
&& wget --no-check-certificate https://github.com/facebook/rocksdb/archive/refs/tags/v"$ROCKSDB_VER".tar.gz \
&& tar zxvf v"$ROCKSDB_VER".tar.gz \
&& cd "$ROCKSDB_DIR" \
&& make clean && make shared_lib \
&& ldd librocksdb.so \
&& jextract --source --header-class-name RocksDB --output /opt/"$OUTPUT_DIR" -t com.silong.foundation.rocksdbffm -I /opt/"$ROCKSDB_DIR"/include/rocksdb -I /opt/"$ROCKSDB_DIR"/include/rocksdb/utilities c.h

