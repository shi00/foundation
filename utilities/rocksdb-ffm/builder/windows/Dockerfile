FROM barkaz/vs2022-vcpkg:latest AS base

LABEL maintainer="louis sin"

WORKDIR "c:\vcpkg"

# update vcpkg
RUN git config --global --add safe.directory C:/vcpkg \
&& git pull
RUN for /f "tokens=1 delims=-" %i in ('git describe --tags') do git checkout %i
RUN @powershell ./bootstrap-vcpkg.bat

#install wget
RUN choco install wget -Y \
&& refreshenv

WORKDIR "c:\Program Files"

# install jextract
RUN wget -t 3 -c --no-check-certificate https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_windows-x64_bin.tar.gz \
&& tar zxvf openjdk-22-jextract+5-33_windows-x64_bin.tar.gz \
&& del openjdk-22-jextract+5-33_windows-x64_bin.tar.gz \
&& choco uninstall wget -y \
&& setx path "%PATH%;C:\Program Files\jextract-22\bin" \
&& refreshenv

FROM base

WORKDIR "c:\vcpkg"

# 添加构建脚本
ADD build.ps1 .

#预制构建工具包到downloads目录下，规避下载速度慢问题
#COPY cmake* downloads
#COPY PowerShell* downloads

# 清理临时文件
RUN del /q/f/s %TEMP%