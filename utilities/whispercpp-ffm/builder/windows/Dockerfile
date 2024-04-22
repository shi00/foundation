FROM barkaz/vs2022-vcpkg:latest

LABEL maintainer="louis sin"

USER containeradministrator

WORKDIR "c:\Program Files"

#install wget
RUN choco install wget -Y \
&& refreshenv

# install jextract
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/3/openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& del openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& setx path "%PATH%;C:\Program Files\jextract-22\bin;C:\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin;C:\BuildTools\MSBuild\Current\Bin\amd64" \
&& refreshenv

WORKDIR "c:\\"

# 添加构建脚本
ADD build.ps1 .

