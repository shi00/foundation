param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"

echo $env:path
echo $env:UserName

echo "Start to build whispercpp ...... "

# 切换到最新的release版本
cd .\vcpkg\

# 构建whisper-cpp并检查命令执行结果
.\vcpkg.exe install whisper-cpp:x64-windows-release
if ($LASTEXITCODE -ne 0)
{
    Write-Error "Failed to build whisper-cpp by vcpkg."
    exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
}

# 定义文件路径（请根据实际情况修改）
$file = ".\installed\x64-windows-release\share\whisper-cpp\whisper-version.cmake"  # 或者 "C:\path\to\a"，比如 "C:\vcpkg\installed\x64-windows-release\share\whisper-cpp\whisper-cpp-version.cmake"

# 检查文件是否存在
if (-Not (Test-Path -Path $file -PathType Leaf))
{
    Write-Host "Error: File not found: $file" -ForegroundColor Red
    exit 1
}

# 读取文件内容
$content = Get-Content -Path $file -Raw

# 使用正则表达式提取 PACKAGE_VERSION "x.y.z" 中的版本号
if ($content -match 'set\(PACKAGE_VERSION\s+"([\d\.]+)"\)')
{
    $version = "v$( $matches[1] )"
    Write-Host "Found whisper.cpp version: $version" -ForegroundColor Green

    git clone https://github.com/ggml-org/whisper.cpp.git
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to clone the repository whisper.cpp."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

    cd .\whisper.cpp\
    git checkout $version
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to checkout to version $version."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

    mkdir build
    cd .\build\
    cmake .. -G "Visual Studio 17 2022" -A x64 -DBUILD_SHARED_LIBS=ON
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to run cmake for whisper.cpp."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

    msbuild whisper.cpp.sln /p:Configuration=Release /p:Platform=x64
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to build whisper.cpp solution."
        exit $LASTEXITCODE  # 以非0代码退出，标识执行失败
    }

    cd .\bin\Release\

    # 检查文件是否存在于当前目录
    $dllFileName = "whisper.dll"
    if (-not (Test-Path -Path $dllFileName -PathType Leaf))
    {
        # 文件不存在时输出错误信息
        Write-Error "$dllFileName not found in the current directory"
        # 以非零代码退出，标识执行失败
        exit 1
    }

    Rename-Item -Path $dllFileName -NewName "$libName.dll"
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "Failed to rename $dllFileName to $libName.dll"
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
}
else
{
    Write-Host "Error: Could not find PACKAGE_VERSION in the file." -ForegroundColor Red
    exit 1
}

# 生成whispercpp的源代码
cd "C:\vcpkg\installed\x64-windows-release\include"
echo "================== Start generate source code for whispercpp =================="
jextract --header-class-name $headerClassName --output c:\$srcDir --target-package $sourcecodePackage --include-dir . whisper.h
$is_src_dir = [System.IO.Directory]::Exists("c:\$srcDir")
if (!$is_src_dir)
{
    echo "Failed to generate code by jextract"
    Exit 1
}

echo "================== Build completed =================="
Start-Sleep -Seconds 5