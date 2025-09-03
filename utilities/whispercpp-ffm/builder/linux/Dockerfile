FROM kuyoh/vcpkg:2025.07.25-ubuntu24.04

LABEL maintainer="louis sin"

USER root

WORKDIR /opt

#安装java相关软件包
RUN ARCH=$(uname -m) && case $ARCH in \
            x86_64)  PACKAGE="https://download.java.net/java/early_access/jextract/22/6/openjdk-22-jextract+6-47_linux-x64_bin.tar.gz https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2404/x86_64/cuda-keyring_1.1-1_all.deb" CUDA_PACKAGE="cuda-keyring_1.1-1_all.deb" JEXTRACT_PACKAGE="openjdk-22-jextract+6-47_linux-x64_bin.tar.gz" ;; \
            aarch64)  PACKAGE="https://download.java.net/java/early_access/jextract/22/6/openjdk-22-jextract+6-47_linux-aarch64_bin.tar.gz https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2404/arm64/cuda-keyring_1.1-1_all.deb" CUDA_PACKAGE="cuda-keyring_1.1-1_all.deb" JEXTRACT_PACKAGE="openjdk-22-jextract+6-47_linux-aarch64_bin.tar.gz" ;; \
            *) echo "Unsupported CPU architecture: $ARCH" && exit 1 ;; \
        esac && echo "Selected package: ${JEXTRACT_PACKAGE}  ${CUDA_PACKAGE}" \
&& apt update && apt install wget && wget -t 3 -nc --no-check-certificate ${PACKAGE} \
&& tar zxvf ${JEXTRACT_PACKAGE} && rm -rf ${JEXTRACT_PACKAGE} \
&& chmod +x ./jextract-22/bin/jextract \
&& dpkg -i ${CUDA_PACKAGE} && rm -rf ${CUDA_PACKAGE} \
&& apt-get update \
&& apt-get -y install dos2unix pax-utils patchelf libopenblas-dev cuda-toolkit-13-0 \
&& apt-get autoremove \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# 设置环境变量
ENV PATH=$PATH:/opt/jextract-22/bin:/usr/local/cuda-13.0/bin
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/cuda-13.0/lib64

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh && dos2unix ./build.sh