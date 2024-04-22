#! /bin/sh
OUTPUT_SRC_DIR=$1
SHARDED_LIB_DIR=$2
SHARDED_LIB_NAME=$3".so"
HEADER_CLASS_NAME=$4
SOURCECODE_PACKAGE=$5

echo "$OS_NAME" "$OS_ARCH" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR" "$HEADER_CLASS_NAME" "$SOURCECODE_PACKAGE"
echo "================== Start building whisper.cpp =================="

git clone https://github.com/ggerganov/whisper.cpp.git &&
cd whisper.cpp &&
make "$SHARDED_LIB_NAME" &&
make stream

#判断文件是否存在
if [ ! -f "$SHARDED_LIB_NAME" ]; then
    echo "Failed to build whisper.cpp."
    exit 1
fi

cp "$SHARDED_LIB_NAME" /opt/"$SHARDED_LIB_DIR"/"$SHARDED_LIB_NAME"

echo "================== Start generate source code for whisper.cpp =================="
jextract --header-class-name "$HEADER_CLASS_NAME" --output /opt/"$OUTPUT_SRC_DIR" --target-package "$SOURCECODE_PACKAGE" --include-dir . whisper.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 5s