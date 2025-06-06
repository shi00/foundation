#! /bin/bash
OUTPUT_SRC_DIR=$1
SHARDED_LIB_DIR=$2
SHARDED_LIB_NAME=$3".so"
HEADER_CLASS_NAME=$4
SOURCECODE_PACKAGE=$5

echo "$OS_NAME" "$OS_ARCH" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR" "$HEADER_CLASS_NAME" "$SOURCECODE_PACKAGE"

#开启lz4和zstd压缩特性
sed -i '/"default-features": \[/{:a;N;/\],/!ba};s/"default-features": \[.*\],/"default-features": \["zlib","zstd","lz4"\],/' ports/rocksdb/vcpkg.json

echo "================== Start building rocksdb =================="

mkdir custom-triplets
cp triplets/x64-linux.cmake custom-triplets/x64-linux-dynamic.cmake
sed -i 's/set(VCPKG_LIBRARY_LINKAGE static)/set(VCPKG_LIBRARY_LINKAGE dynamic)/g' custom-triplets/x64-linux-dynamic.cmake
./vcpkg install rocksdb:x64-linux-dynamic --overlay-triplets=custom-triplets

#判断文件是否存在
if [ ! -f installed/x64-linux-dynamic/lib/"$SHARDED_LIB_NAME" ]; then
    echo "Failed to build rocksdb."
    exit 1
fi

cp installed/x64-linux-dynamic/lib/"$SHARDED_LIB_NAME" /opt/"$SHARDED_LIB_DIR"
cp installed/x64-linux-dynamic/lib/"libz.so" /opt/"$SHARDED_LIB_DIR"
cp installed/x64-linux-dynamic/lib/"libzstd.so" /opt/"$SHARDED_LIB_DIR"
cp installed/x64-linux-dynamic/lib/"liblz4.so" /opt/"$SHARDED_LIB_DIR"

echo "================== Start generate source code for rocksdb =================="
jextract --header-class-name "$HEADER_CLASS_NAME" --output /opt/"$OUTPUT_SRC_DIR" --target-package "$SOURCECODE_PACKAGE" --include-dir ./installed/x64-linux-dynamic/include/rocksdb ./installed/x64-linux-dynamic/include/rocksdb/c.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 5s