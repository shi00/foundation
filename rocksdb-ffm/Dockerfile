FROM centos:7

LABEL maintainer="louis sin"

USER root

WORKDIR /opt

ENV TZ=Asia/Shanghai

#安装常用软件包
RUN yum -y install epel-release \
#&& sed -i 's/metalink=/#metalink=/g' /etc/yum.repos.d/epel.repo \
#&& sed -i 's/#baseurl=/baseurl=/g' /etc/yum.repos.d/epel.repo \
&& yum -y update \
&& yum -y groupinstall 'Development Tools' \
&& yum -y install wget snappy snappy-devel zlib zlib-devel bzip2 bzip2-devel lz4-devel libasan which python3 libzstd-devel gflags-devel \
&& yum -y install centos-release-scl \
&& yum -y install devtoolset-11 \
&& yum clean all \
&& rm -rf /var/cache/yum \
&& ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
&& echo "source /opt/rh/devtoolset-11/enable" >> ~/.bashrc \
&& echo "source /etc/profile" >> ~/.bashrc
SHELL ["/bin/bash", "--login", "-c"]
ENTRYPOINT ["/usr/bin/bash", "--login", "-c"]
#安装常用软件包

#安装java相关软件包
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/1/openjdk-20-jextract+1-2_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-20-jextract+1-2_linux-x64_bin.tar.gz \
&& wget -t 3 -c --no-check-certificate https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-21_linux-x64_bin.tar.gz \
&& wget -t 3 -c --no-check-certificate https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz \
&& tar zxvf apache-maven-3.9.4-bin.tar.gz \
&& echo "export M2_HOME=/opt/apache-maven-3.9.4" >> /etc/profile \
&& echo "export JAVA_HOME=/opt/jdk-21" >> /etc/profile \
&& echo "export PATH=$PATH:/opt/jdk-21/bin:/opt/apache-maven-3.9.4/bin:/opt/jextract-20/bin" >> /etc/profile \
&& rm -rf *.tar.gz
#安装java相关软件包

# 添加构建脚本
ADD build.sh .
RUN chmod +x /opt/build.sh