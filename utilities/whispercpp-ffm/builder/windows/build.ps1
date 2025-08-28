param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage, [string]$target_arch)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"
Write-Output "targetArch: $target_arch"
$originDllFileName = "whisper.dll"
echo $env:path
echo $env:UserName

echo "Start to build whispercpp ...... "

git clone https://github.com/ggml-org/whisper.cpp.git
if ($LASTEXITCODE -ne 0)
{
    Write-Error "Failed to clone the repository whisper.cpp."
    exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
}

# 切换到最新的release版本
cd .\whisper.cpp\
$last_tag = git describe --tags
$last_tag = $last_tag.Split("-")[0]
git checkout $last_tag
if ($LASTEXITCODE -ne 0)
{
    Write-Error "Failed to checkout to the latest tag $last_tag."
    exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
}

# 检查是否有 NVIDIA 显卡
$nvidiaGpu = Get-CimInstance -ClassName Win32_VideoController | Where-Object { $_.Name -match "NVIDIA" }
if (-not $nvidiaGpu)
{
    Write-Host "Enable CPU compilation options ......"
    mkdir build
    cd .\build\
    cmake .. -G "Visual Studio 17 2022" -A $target_arch -DBUILD_SHARED_LIBS=ON
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to run cmake for whisper.cpp."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

    msbuild whisper.cpp.sln /p:Configuration=Release /p:Platform=$target_arch /m:4
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to build whisper.cpp solution."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

}
else
{
    # 检查 nvidia-smi 命令是否可用（验证驱动安装）
    try
    {
        $nvidiaSmiOutput = nvidia-smi 2>&1
        if ($LASTEXITCODE -ne 0)
        {
            Write-Error "NVIDIA driver not found or driver installation incomplete (nvidia-smi execution failed)." -ForegroundColor Red
            exit 1
        }

        Write-Host "Enable CUDA compilation options ......"

    }
    catch
    {
        Write-Error "The nvidia-smi command was not found, please ensure that the NVIDIA driver is installed." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Successfully built whisper.cpp!!!"

cd .\bin\Release\

# 检查文件是否存在于当前目录
if (-not (Test-Path -Path $originDllFileName -PathType Leaf))
{
    # 文件不存在时输出错误信息
    Write-Error "$originDllFileName not found in the current directory"
    # 以非零代码退出，标识执行失败
    exit 1
}

Rename-Item -Path $originDllFileName -NewName "$libName.dll"
if ($LASTEXITCODE -ne 0)
{
    Write-Error "Failed to rename $originDllFileName to $libName.dll"
    exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
}

# 获取当前目录下所有的.dll文件
$dllFiles = Get-ChildItem -Path . -Filter *.dll -File
if ($dllFiles.Count -eq 0)
{
    Write-Warning "No DLL files found in the current directory."
    exit 0
}

# 复制所有DLL文件到目标目录（覆盖同名文件）
$dll_target_dir = "c:\$dllDir"
foreach ($file in $dllFiles)
{
    $destinationPath = Join-Path -Path $dll_target_dir -ChildPath $file.Name

    # 使用-Force参数强制覆盖已存在的文件
    Copy-Item -Path $file.FullName -Destination $destinationPath -Force
    Write-Host "Copied (overwritten if exists): $( $file.Name ) to $dll_target_dir"
}

Write-Host "DLL copy operation completed. Total files processed: $( $dllFiles.Count )"

# 生成whispercpp的源代码
cd "C:\whisper.cpp\include"
echo "================== Start generate source code for whispercpp =================="
jextract --header-class-name $headerClassName --output c:\$srcDir --target-package $sourcecodePackage  --include-dir ..\ggml\include --include-dir . whisper.h
$is_src_dir = [System.IO.Directory]::Exists("c:\$srcDir")
if (!$is_src_dir)
{
    echo "Failed to generate code by jextract"
    Exit 1
}

echo "================== Build completed =================="
Start-Sleep -Seconds 5