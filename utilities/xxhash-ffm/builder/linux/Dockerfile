FROM centos:7 AS base

LABEL maintainer="louis sin"

USER root

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /opt

#安装常用软件包
RUN yum -y install epel-release \
#&& sed -i 's/metalink=/#metalink=/g' /etc/yum.repos.d/epel.repo \
#&& sed -i 's/#baseurl=/baseurl=/g' /etc/yum.repos.d/epel.repo \
&& yum -y install https://packages.endpointdev.com/rhel/7/os/x86_64/endpoint-repo.x86_64.rpm \
&& yum -y update \
&& yum -y groupinstall 'Development Tools' \
&& yum -y install wget which git zip unzip centos-release-scl devtoolset-11 \
&& yum clean all \
&& rm -rf /var/cache/yum \
&& echo "source /opt/rh/devtoolset-11/enable" >> ~/.bashrc \
&& echo "source /etc/profile" >> ~/.bashrc
SHELL ["/bin/bash", "--login", "-c"]
ENTRYPOINT ["/usr/bin/bash", "--login", "-c"]
#安装常用软件包

#安装java相关软件包
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-21-jextract+1-2_linux-x64_bin.tar.gz \
&& mv jextract-21 jextract \
&& wget -t 3 -c --no-check-certificate https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz \
&& tar zxvf jdk-21_linux-x64_bin.tar.gz \
&& mv jdk-21 jdk \
&& wget -t 3 -c --no-check-certificate https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz \
&& tar zxvf apache-maven-3.9.4-bin.tar.gz \
&& mv apache-maven-3.9.4 apache-maven \
&& wget -t 3 -c --no-check-certificate https://github.com/microsoft/vcpkg/archive/refs/tags/2023.08.09.tar.gz \
&& tar zxvf 2023.08.09.tar.gz \
&& mv vcpkg-2023.08.09 vcpkg \
&& ./vcpkg/bootstrap-vcpkg.sh \
&& ./vcpkg/vcpkg integrate install \
&& echo "export M2_HOME=/opt/apache-maven" >> /etc/profile \
&& echo "export JAVA_HOME=/opt/jdk" >> /etc/profile \
&& echo "export PATH=$PATH:/opt/jdk/bin:/opt/apache-maven/bin:/opt/jextract/bin" >> /etc/profile \
&& rm -rf *.tar.gz
#安装java相关软件包

FROM base

WORKDIR /opt

# 添加构建脚本
ADD build.sh .
RUN chmod +x ./build.sh