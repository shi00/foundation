param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"
echo $env:path
echo $env:UserName

echo "Start to build whispercpp ...... "
git clone https://github.com/ggerganov/whisper.cpp.git
$is_src_dir = [System.IO.Directory]::Exists("c:\whisper.cpp")
if (!$is_src_dir)
{
    echo "Failed to clone whispercpp from github."
    Exit 1
}

cd .\whisper.cpp\
cmake . --fresh
msbuild ALL_BUILD.vcxproj /p:Configuration=Release
Rename-Item -Path bin\Release\whisper.dll -NewName “$libName.dll”
copy bin\Release\$libName.dll c:\$dllDir

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