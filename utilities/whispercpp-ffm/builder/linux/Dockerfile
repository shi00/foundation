FROM kuyoh/vcpkg:2025.07.25-ubuntu24.04

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

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
&& rm -rf *.tar.gz \ && chmod +x ./jextract-22/bin/jextract \
&& apt-get autoremove \
&& apt-get clean

# 设置环境变量
ENV PATH $PATH:/opt/jextract-22/bin
#安装java相关软件包

WORKDIR /opt/vcpkg
RUN git pull origin master \
&& ./bootstrap-vcpkg.sh

WORKDIR /opt

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh