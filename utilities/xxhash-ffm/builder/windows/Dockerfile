FROM barkaz/vs2022-vcpkg:latest AS base

LABEL maintainer="louis sin"

WORKDIR "c:\vcpkg"

# update chocolately package manager
RUN @powershell ./vcpkg upgrade  --no-dry-run

RUN choco install openjdk -Y \
&& refreshenv

RUN choco install maven -Y \
&& refreshenv

RUN choco install wget -Y \
&& refreshenv

WORKDIR "c:\Program Files"

RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_windows-x64_bin.tar.gz \
&& tar zxvf openjdk-21-jextract+1-2_windows-x64_bin.tar.gz \
&& del openjdk-21-jextract+1-2_windows-x64_bin.tar.gz \
&& setx path "%PATH%;C:\Program Files\jextract-21\bin" \
&& refreshenv

WORKDIR "c:\vcpkg"

# 添加构建脚本
ADD build.bat .
