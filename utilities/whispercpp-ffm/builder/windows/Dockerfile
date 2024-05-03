FROM barkaz/vs2022-vcpkg:latest

LABEL maintainer="louis sin"

USER containeradministrator

WORKDIR "c:\Program Files"

#install wget
RUN choco install wget -Y \
&& refreshenv

# install jextract, python310
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/3/openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& del openjdk-22-jextract+3-13_windows-x64_bin.tar.gz \
&& choco uninstall wget -y \
&& choco install python -y --version=3.10 \
&& setx path "%PATH%;C:\Python310;C:\Program Files\jextract-22\bin;C:\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin;C:\BuildTools\MSBuild\Current\Bin\amd64" \
&& refreshenv

WORKDIR "c:\\"

# 添加构建脚本
ADD build.ps1 .

# 清理临时文件
RUN del /q/f/s %TEMP%