FROM kuyoh/vcpkg:latest AS base

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /opt

#安装java相关软件包
RUN apt update && apt install libsdl2-dev -y && apt install wget && wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/3/openjdk-22-jextract+3-13_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+3-13_linux-x64_bin.tar.gz \
&& mv jextract-22 jextract \
&& rm -rf *.tar.gz
ENV PATH $PATH:/opt/jextract/bin
#安装java相关软件包

FROM base

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh