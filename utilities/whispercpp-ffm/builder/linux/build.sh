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

# 执行 nvidia-smi 并捕获退出码（&> /dev/null 静默执行，不输出内容）
nvidia-smi &> /dev/null

# 判断退出码
if [ $? -eq 0 ]; then
    echo "Nvidia GPU has been detected and CUDA installation has started."
    # 暂不支持
    exit 1
else
    echo "Nvidia GPU not detected, enabling CPU mode."

    # 进入vcpkg目录，完成vcpkg的更新和初始化
    cd "vcpkg"

    git pull origin master
    if [ $? -eq 0 ]; then
        echo "vcpkg updated successfully."
    else
        echo "Failed to update vcpkg."
        exit 1
    fi

    ./bootstrap-vcpkg.sh
    if [ $? -eq 0 ]; then
        echo "vcpkg bootstrapped successfully."
    else
        echo "Failed to bootstrap vcpkg."
        exit 1
    fi

    vcpkg install whisper-cpp:x64-linux-dynamic
    cd "installed/x64-linux-dynamic/lib"

    #判断构建whisper-cpp是否成功
    if [ ! -f "./$SHARDED_LIB_NAME" ]; then
       echo "Failed to build whisper-cpp."
       exit 1
    fi

    # 拷贝所有so文件至目标目录
    find . -maxdepth 1 \( -type f -o -type l \) -name "*.so" -exec cp {} /opt/"$SHARDED_LIB_DIR" \;

    # 进入include目录
    cd "../include/"

    echo "================== Start generate source code for whispercpp =================="
    jextract --header-class-name "$HEADER_CLASS_NAME" --output /opt/"$OUTPUT_SRC_DIR" --target-package "$SOURCECODE_PACKAGE" --include-dir . whisper.h
    if [ ! -d "/opt/$OUTPUT_SRC_DIR" ];then
        echo "Failed to generate code by jextract"
        exit 1
    fi

    echo "================== Build completed =================="
    sleep 5s
    exit 0
fi