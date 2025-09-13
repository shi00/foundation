FROM kuyoh/vcpkg:2025.07.25-ubuntu24.04 AS base

LABEL maintainer="louis sin"

USER root

WORKDIR /opt

#安装java相关软件包
ARG BASE_URL="https://download.java.net/java/early_access/jextract/22/6"
RUN ARCH=$(uname -m) && case $ARCH in \
        x86_64)  PACKAGE="openjdk-22-jextract+6-47_linux-x64_bin.tar.gz" ;; \
        aarch64)  PACKAGE="openjdk-22-jextract+6-47_linux-aarch64_bin.tar.gz" ;; \
        *) echo "Unsupported CPU architecture: $ARCH" && exit 1 ;; \
    esac && echo "Selected package: ${PACKAGE}" \
    && apt update && apt install wget && wget -t 3 -c --no-check-certificate "${BASE_URL}/${PACKAGE}" \
    && tar zxvf "${PACKAGE}" \
    && chmod +x ./jextract-22/bin/jextract \
    && rm -rf *.tar.gz

# 设置环境变量
ENV PATH $PATH:/opt/jextract-22/bin
#安装java相关软件包

# update vcpkg
RUN cd vcpkg && git pull origin master \
&& ./bootstrap-vcpkg.sh

FROM base

WORKDIR /opt/vcpkg

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh