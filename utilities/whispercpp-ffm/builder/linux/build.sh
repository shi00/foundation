#! /bin/bash
#
# /*
#  * Licensed to the Apache Software Foundation (ASF) under one
#  * or more contributor license agreements.  See the NOTICE file
#  * distributed with this work for additional information
#  * regarding copyright ownership.  The ASF licenses this file
#  * to you under the Apache License, Version 2.0 (the
#  * "License"); you may not use this file except in compliance
#  * with the License.  You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing,
#  * software distributed under the License is distributed on an
#  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  * KIND, either express or implied.  See the License for the
#  * specific language governing permissions and limitations
#  * under the License.
#  */
#

OUTPUT_SRC_DIR=$1
SHARDED_LIB_DIR=$2
SHARDED_LIB_NAME=$3".so"
HEADER_CLASS_NAME=$4
SOURCECODE_PACKAGE=$5

echo "$OS_NAME" "$OS_ARCH" "$OUTPUT_SRC_DIR" "$SHARDED_LIB_DIR" "$HEADER_CLASS_NAME" "$SOURCECODE_PACKAGE"
echo "================== Start building whispercpp =================="

include_dirs=""

# Ubuntu 24.04系统库路径模式（固定路径，无需额外配置）
SYSTEM_LIB_PATHS=(
    "/lib/x86_64-linux-gnu/"    # x86_64系统库
    "/lib32/"                   # 32位兼容库
    "/lib64/"                   # 64位系统库
    "/usr/lib/x86_64-linux-gnu/" # 用户系统库
    "/usr/lib32/"
    "/usr/lib64/"
)

download_and_checkout_whispercpp() {
    git clone https://github.com/ggml-org/whisper.cpp.git || {
      echo "Failed to pull whisper.cpp from repo."
      exit 1
    }
    cd whisper.cpp
    last_tag=$(git describe --tags 2>/dev/null)
    last_tag="${last_tag%%-*}"
    git checkout "$last_tag"
    echo "Checked out to the latest tag of whisper.cpp: $last_tag"
}

collect_build_libs() {
    DEST_DIR="/opt/$SHARDED_LIB_DIR"
    DEST_LIB="build/src/libwhisper.so"
    cp -vL "$DEST_LIB" "$DEST_DIR/"
    mv -v "$DEST_DIR/libwhisper.so" "$DEST_DIR/$SHARDED_LIB_NAME"
    DEST_LIB="$DEST_DIR/$SHARDED_LIB_NAME"

    # 收集非系统依赖（利用Ubuntu固定路径特性）
    echo "Collecting non-system dependencies for $DEST_LIB"
    lddtree -l "$DEST_LIB" | grep -v 'not found' | awk '{print $1}' | sort -u > "$DEST_DIR/deps.tmp"

    # 筛选并复制非系统库
    grep -vFf <(printf "%s\n" "${SYSTEM_LIB_PATHS[@]}") "$DEST_DIR/deps.tmp" > "$DEST_DIR/non_system_deps.tmp"

    if [ -s "$DEST_DIR/non_system_deps.tmp" ]; then
        xargs -I {} cp -vL {} "$DEST_DIR/" < "$DEST_DIR/non_system_deps.tmp"
    fi

    # 批量修改依赖路径
    echo "Modifying RPATH and dependencies for shared libraries in $DEST_DIR"
    find "$DEST_DIR" -name "*.so*" -type f | while read -r lib; do
        echo "processing: $(basename "$lib")"
        patchelf --set-rpath '$ORIGIN' "$lib"
        # 替换依赖名为当前目录中的库
        ldd "$lib" | awk '/=>/ {print $1}' | while read -r dep; do
            dep_base=$(basename "$dep")
            [ -f "$DEST_DIR/$dep_base" ] && patchelf --replace-needed "$dep" "$dep_base" "$lib"
        done
    done

    rm -f "$DEST_DIR/deps.tmp" "$DEST_DIR/non_system_deps.tmp"
    include_dirs="$(pwd)/ggml/include"
    cd include
}

#下载whisper代码并切换至最新分支
download_and_checkout_whispercpp

# 执行 nvidia-smi 并捕获退出码（&> /dev/null 静默执行，不输出内容）
if nvidia-smi &> /dev/null; then
    echo "Nvidia GPU has been detected and CUDA installation has started."
    cmake -B build -DGGML_CUDA=1 -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release
    cmake --build build -j4 --config Release
elif ldconfig -p | grep -q openblas; then
    echo "Nvidia GPU not detected, enabling OpenBLAS mode."
    cmake -B build -DGGML_BLAS=1 -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release
    cmake --build build -j4 --config Release
else
    echo "Neither Nvidia GPU nor OpenBLAS detected, proceeding with CPU build."
    # 进入vcpkg目录，完成vcpkg的更新和初始化
   cmake -B build -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release
   cmake --build build -j4 --config Release
fi

# 收集构建结果
collect_build_libs

echo "================== Start generate source code for whispercpp =================="
jextract --header-class-name "$HEADER_CLASS_NAME" --output /opt/"$OUTPUT_SRC_DIR" --target-package "$SOURCECODE_PACKAGE" --include-dir "$include_dirs" whisper.h
if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
     echo "Failed to generate code by jextract"
     exit 1
fi

echo "================== Build completed =================="
sleep 5s
exit 0