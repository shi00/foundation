FROM barkaz/vs2022-vcpkg:latest

LABEL maintainer="louis sin"

USER containeradministrator

WORKDIR "c:\\Program Files"

# download jextract openblas
RUN choco install wget -Y \
&& wget -t 3 -c --no-check-certificate "https://download.java.net/java/early_access/jextract/22/6/openjdk-22-jextract+6-47_windows-x64_bin.tar.gz" \
&& wget -t 3 -c --no-check-certificate -O "OpenBLAS-0.3.30-x64.zip" "https://cyfuture.dl.sourceforge.net/project/openblas/v0.3.30/OpenBLAS-0.3.30-x64.zip?viasf=1" \
&& choco uninstall wget -y

# install jextract openblas
RUN tar zxvf openjdk-22-jextract+6-47_windows-x64_bin.tar.gz \
&& mkdir OpenBLAS-0.3.30-x64 && tar -xf OpenBLAS-0.3.30-x64.zip -C OpenBLAS-0.3.30-x64 \
&& del openjdk-22-jextract+6-47_windows-x64_bin.tar.gz OpenBLAS-0.3.30-x64.zip \
RUN setx path "%PATH%;C:\Program Files\jextract-22\bin;C:\Program Files\OpenBLAS-0.3.30-x64\bin;C:\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin;C:\BuildTools\MSBuild\Current\Bin\amd64" \
&& refreshenv

WORKDIR "c:\\"

# 添加构建脚本
ADD build.ps1 .

# 清理临时文件
RUN del /q/f/s %TEMP%