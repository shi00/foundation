FROM kuyoh/vcpkg:latest

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /opt

#安装java相关软件包
RUN apt update && apt install wget && wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/4/openjdk-22-jextract+4-30_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+4-30_linux-x64_bin.tar.gz \
&& rm -rf *.tar.gz \ && chmod +x ./jextract-22/jextract \
&& apt install software-properties-common -y \
&& add-apt-repository ppa:deadsnakes/ppa \
&& apt install python3.10-venv -y \
&& apt-get autoremove \
&& apt-get clean

# 设置环境变量
ENV PATH $PATH:/opt/jextract-22
#安装java相关软件包

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh