#! /bin/bash
OUTPUT_SRC_DIR=$1
SHARDED_LIB_DIR=$2
SHARDED_LIB_NAME=$3".so"
HEADER_CLASS_NAME=$4
SOURCECODE_PACKAGE=$5
BUILD_PARAMS=$6

echo "$OS_NAME" "$OS_ARCH" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR" "$HEADER_CLASS_NAME" "$SOURCECODE_PACKAGE" "$BUILD_PARAMS"
echo "================== Start building whispercpp =================="

git clone https://github.com/ggerganov/whisper.cpp.git
if [ ! -d "./whisper.cpp/" ];then
  echo "Failed to clone whisper.cpp from github."
  exit 1
fi

cd "whisper.cpp"

if echo "$BUILD_PARAMS" | grep -iqF "openvino"; then
 echo "Start downloading OpenVINO......"
 openvino_file=${BUILD_PARAMS##*/} #substring 最后一个/后的字符串
 openvino_dir=${openvino_file%.*}  #substring 最后一个.前的字符串
 openvino_path="/opt/whisper.cpp/$openvino_dir/runtime/cmake/"
 echo "openvino_file=$openvino_file"
 echo "openvino_dir=$openvino_dir"
 echo "openvino_url=$BUILD_PARAMS"
 echo "openvino_path=$openvino_path"
 wget -t 3 -c --no-check-certificate "$BUILD_PARAMS"  # 下载openvino
 if [ ! -f "./$openvino_file" ];then
   echo "Failed to download $openvino_file."
   exit 1
 fi

 #解压openvino运行时包
 tar zxvf "$openvino_file"
 rm -rf "$openvino_file"

 cd "$openvino_dir"
 yes | source ./install_dependencies/install_openvino_dependencies.sh
 cd ".."

 cd "models"
 python3 -m venv openvino_conv_env && source "openvino_conv_env/bin/activate" && python -m pip install --upgrade pip  && pip install -r requirements-openvino.txt
 source "/opt/whisper.cpp/$openvino_dir/setupvars.sh"

 # 开始构建
 cd ".."
 sed -Ei "s|# Add path to modules|list(APPEND CMAKE_PREFIX_PATH \"$openvino_path\")|" CMakeLists.txt

 cmake -B build -DWHISPER_OPENVINO=1
 cmake --build build -j --config Release

 #判断文件是否存在
 if [ ! -f "./build/$SHARDED_LIB_NAME" ]; then
     echo "Failed to build whispercpp."
     exit 1
 fi

 cp "./build/$SHARDED_LIB_NAME" /opt/"$SHARDED_LIB_DIR"/"$SHARDED_LIB_NAME"

elif echo "$BUILD_PARAMS" | grep -iqF "cuda"; then
  echo "CUDA is not supported now."
  exit 1
else
  make "$SHARDED_LIB_NAME" && make stream

  #判断文件是否存在
  if [ ! -f "$SHARDED_LIB_NAME" ]; then
      echo "Failed to build whispercpp."
      exit 1
  fi

  cp "$SHARDED_LIB_NAME" /opt/"$SHARDED_LIB_DIR"/"$SHARDED_LIB_NAME"
fi

echo "================== Start generate source code for whispercpp =================="
jextract --header-class-name "$HEADER_CLASS_NAME" --output /opt/"$OUTPUT_SRC_DIR" --target-package "$SOURCECODE_PACKAGE" --include-dir . whisper.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
  echo "Failed to generate code by jextract"
  exit 1
fi

echo "================== Build completed =================="
sleep 5s