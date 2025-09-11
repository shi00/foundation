param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage, [string]$target_arch)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"

cd .\vcpkg\
.\vcpkg.exe install portaudio:x64-windows

Write-Host "Successfully built portaudio!!!"

cd .\installed\x64-windows\bin\
$originDllFileName = "portaudio.dll"

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
    exit 1
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
cd ..\include\
echo "================== Start generate source code for whispercpp =================="
jextract --header-class-name $headerClassName --output c:\$srcDir --target-package $sourcecodePackage  --include-dir . portaudio.h
$is_src_dir = [System.IO.Directory]::Exists("c:\$srcDir")
if (!$is_src_dir)
{
    echo "Failed to generate code by jextract"
    Exit 1
}

echo "================== Build completed =================="
Start-Sleep -Seconds 5