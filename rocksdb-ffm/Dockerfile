FROM centos:7

#############
##  SETUP  ##
#############
RUN yum -y update \
&& yum -y groupinstall 'Development Tools' \
&& yum -y install wget curl snappy snappy-devel zlib zlib-devel bzip2 bzip2-devel lz4-devel libasan which \
&& yum clean all \
&& rm -rf /var/cache/yum

# Bump to gcc-11
RUN yum -y update \
&& yum -y install centos-release-scl \
&& yum -y install devtoolset-11 \
&& yum clean all \
&& echo "source /opt/rh/devtoolset-11/enable" >> /etc/bashrc
SHELL ["/bin/bash", "--login", "-c"]
ENTRYPOINT ["/usr/bin/bash", "--login", "-c"]
# RUN gcc --version

RUN yum -y install epel-release \
&& yum -y install libzstd-devel gflags \
&& yum clean all \
&& rm -rf /var/cache/yum

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD build.sh /opt
RUN chmod +x /opt/build.sh
