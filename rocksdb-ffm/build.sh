#! /bin/sh
ROCKSDB_VER=$1
cd /opt
wget --no-check-certificate https://github.com/facebook/rocksdb/archive/refs/tags/v"$ROCKSDB_VER".tar.gz
tar zxvf v"$ROCKSDB_VER".tar.gz
cd rocksdb-"$ROCKSDB_VER"
make clean && make shared_lib
lld librocksdb.so

