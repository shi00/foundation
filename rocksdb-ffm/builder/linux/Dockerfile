FROM acgetchell/vcpkg-image:latest AS base

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /opt

#安装java相关软件包
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-21-jextract+1-2_linux-x64_bin.tar.gz \
&& mv jextract-21 jextract \
&& rm -rf *.tar.gz \
ENV PATH $PATH:/opt/jextract/bin
#安装java相关软件包

FROM base

WORKDIR /vcpkg

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh