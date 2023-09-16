FROM centos:7

#安装常用软件包
RUN yum -y update \
&& yum -y groupinstall 'Development Tools' \
&& yum -y install wget snappy snappy-devel zlib zlib-devel bzip2 bzip2-devel lz4-devel libasan which \
&& yum clean all \
&& rm -rf /var/cache/yum
#安装常用软件包

#安装 gcc-11
RUN yum -y update \
&& yum -y install centos-release-scl \
&& yum -y install devtoolset-11 \
&& yum clean all \
&& echo "source /opt/rh/devtoolset-11/enable" >> /etc/bashrc
SHELL ["/bin/bash", "--login", "-c"]
ENTRYPOINT ["/usr/bin/bash", "--login", "-c"]
#安装 gcc-11

#安装 libzstd gflags
RUN yum -y install epel-release \
&& yum -y install libzstd-devel gflags \
&& yum clean all \
&& rm -rf /var/cache/yum
#安装 libzstd gflags

#安装 jextract
WORKDIR /opt
RUN wget --no-check-certificate https://download.java.net/java/early_access/jextract/1/openjdk-20-jextract+1-2_linux-x64_bin.tar.gz \
&& tar zxvf openjdk-20-jextract+1-2_linux-x64_bin.tar.gz \
&& echo "export PATH=$PATH:/opt/jextract-20/bin" >> /etc/profile \
&& source /etc/profile
#安装 jextract

# 预制构建脚本
ADD build.sh .
RUN chmod +x /opt/build.sh

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
