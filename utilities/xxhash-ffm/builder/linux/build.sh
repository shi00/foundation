#! /bin/sh
OUTPUT_SRC_DIR=$1
SHARDED_LIB_DIR=$2
OS_NAME=$(echo $(uname -s) | tr '[A-Z]' '[a-z]')   # 获取操作系统名
OS_ARCH=$(uname -p)                  # 获取操作系统架构
SHARDED_LIB_NAME=$3".so"

source /etc/profile

echo "$OS_NAME" "$OS_ARCH" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR"
echo "================== Start building xxHash =================="
cd vcpkg
mkdir custom-triplets
cp triplets/x64-linux.cmake custom-triplets/x64-linux-dynamic.cmake
sed -i 's/set(VCPKG_LIBRARY_LINKAGE static)/set(VCPKG_LIBRARY_LINKAGE dynamic)/g' custom-triplets/x64-linux-dynamic.cmake
./vcpkg install xxhash:x64-linux-dynamic --overlay-triplets=custom-triplets

mkdir -p /opt/"$SHARDED_LIB_DIR"/native/"$OS_NAME"/"$OS_ARCH"
cp installed/x64-linux-dynamic/lib/"$SHARDED_LIB_NAME" /opt/"$SHARDED_LIB_DIR"/native/"$OS_NAME"/"$OS_ARCH"/"$SHARDED_LIB_NAME"

echo "================== Start generate source code for xxHash =================="
jextract --source --header-class-name xxHash --output /opt/"$OUTPUT_SRC_DIR" -t com.silong.foundation.utilities.xxhash.generated -I /opt/vcpkg/installed/x64-linux-dynamic/include /opt/vcpkg/installed/x64-linux-dynamic/include/xxhash.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 10s