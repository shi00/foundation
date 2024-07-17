FROM kuyoh/vcpkg:latest AS base

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /opt

#安装java相关软件包
RUN apt update && apt install wget && wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/4/openjdk-22-jextract+4-30_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+4-30_linux-x64_bin.tar.gz \
&& chmod +x ./jextract-22/jextract \
&& rm -rf *.tar.gz

# 设置环境变量
ENV PATH $PATH:/opt/jextract-22/
#安装java相关软件包

# update vcpkg
RUN rm -rf vcpkg \
&& git clone https://github.com/microsoft/vcpkg.git \
&& ./vcpkg/bootstrap-vcpkg.sh

FROM base

WORKDIR /opt/vcpkg

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh