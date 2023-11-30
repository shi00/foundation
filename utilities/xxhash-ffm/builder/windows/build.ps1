param([string]$srcDir, [string]$dllDir, [string]$libName)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"

$dllFile_x64 = "C:\vcpkg\installed\x64-windows\bin\xxhash.dll"
$dllFile_x86 = "C:\vcpkg\installed\x86-windows\bin\xxhash.dll"
$includeDir = ""

echo "Start to build xxHash ...... "
.\vcpkg install xxhash

$is_dll_x86_exist = [System.IO.File]::Exists($dllFile_x86)
$is_dll_x64_exist = [System.IO.File]::Exists($dllFile_x64)

if ($is_dll_x64_exist)
{
    Move-Item -Path $dllFile_x64 -Destination c:\$dllDir\
    Rename-Item -Path c:\$dllDir\xxhash.dll -NewName $libName.dll
    $includeDir = "C:\vcpkg\installed\x64-windows\include"
    echo "xxhash:x64-windows has been successfully built."
}
elseif($is_dll_x86_exist)
{
    Move-Item -Path $dllFile_x86 -Destination c:\$dllDir\
    Rename-Item -Path c:\$dllDir\xxhash.dll -NewName "$libName.dll"
    $includeDir = "C:\vcpkg\installed\x86-windows\include"
    echo "xxhash:x86-windows has been successfully built."
}
else
{
    echo "Failed to build xxhash."
    Exit 1
}

echo "================== Start generate source code for xxHash =================="
jextract --source --header-class-name xxHash --output c:\$srcDir -t com.silong.foundation.utilities.xxhash.generated -I $includeDir $includeDir\xxhash.h
$is_src_dir = [System.IO.Directory]::Exists($srcDir)
if (!$is_src_dir)
{
    echo "Failed to generate code by jextract"
    Exit 1
}

echo "================== Build completed =================="
Start-Sleep -Seconds 5