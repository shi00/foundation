param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"
echo $env:path
echo $env:UserName

#开启lz4和zstd压缩特性
$jsonContent = Get-Content -Path ".\ports\rocksdb\vcpkg.json"
$jsonObj = $jsonContent | ConvertFrom-Json
$jsonObj."default-features" += 'lz4'
$jsonObj."default-features" += 'zstd'
$jsonObj | ConvertTo-Json -Depth 10 | Out-File ".\ports\rocksdb\vcpkg.json" -Force -Encoding Default
#$jsonContent = Get-Content -Path "C:\vcpkg\ports\rocksdb\vcpkg.json"
#echo $jsonContent


$dllRocksdb_x64 = ".\installed\x64-windows\bin\rocksdb-shared.dll"
$dllRocksdb_x86 = ".\installed\x86-windows\bin\rocksdb-shared.dll"
$dllLz4_x64 = ".\installed\x64-windows\bin\lz4.dll"
$dllLz4_x86 = ".\installed\x86-windows\bin\lz4.dll"
$dllZSTD_x64 = ".\installed\x64-windows\bin\zstd.dll"
$dllZSTD_x86 = ".\installed\x86-windows\bin\zstd.dll"
$dllZlib_x64 = ".\installed\x64-windows\bin\zlib1.dll"
$dllZlib_x86 = ".\installed\x86-windows\bin\zlib1.dll"
$includeDir = ""

echo "Start to build rocksdb ...... "
.\vcpkg install rocksdb

$is_rocksdb_dll_x86_exist = [System.IO.File]::Exists($dllRocksdb_x86)
$is_rocksdb_dll_x64_exist = [System.IO.File]::Exists($dllRocksdb_x64)

$is_lz4_dll_x86_exist = [System.IO.File]::Exists($dllLz4_x86)
$is_lz4_dll_x64_exist = [System.IO.File]::Exists($dllLz4_x64)

$is_zstd_dll_x86_exist = [System.IO.File]::Exists($dllZSTD_x86)
$is_zstd_dll_x64_exist = [System.IO.File]::Exists($dllZSTD_x64)

$is_zlib_dll_x86_exist = [System.IO.File]::Exists($dllZlib_x86)
$is_zlib_dll_x64_exist = [System.IO.File]::Exists($dllZlib_x64)

if ($is_zlib_dll_x64_exist)
{
    Move-Item -Path "$dllZlib_x64" -Destination "c:\$dllDir\"
    echo "zlib:x64-windows has been successfully built."
}
elseif ($is_zlib_dll_x86_exist)
{
    Move-Item -Path "$dllZlib_x86" -Destination "c:\$dllDir\"
    echo "zlib:x86-windows has been successfully built."
}
else
{
    echo "Failed to build zlib."
    Exit 1
}

if ($is_lz4_dll_x64_exist)
{
    Move-Item -Path "$dllLz4_x64" -Destination "c:\$dllDir\"
    echo "lz4:x64-windows has been successfully built."
}
elseif ($is_lz4_dll_x86_exist)
{
    Move-Item -Path "$dllLz4_x86" -Destination "c:\$dllDir\"
    echo "lz4:x86-windows has been successfully built."
}
else
{
    echo "Failed to build lz4."
    Exit 1
}

if ($is_zstd_dll_x64_exist)
{
    Move-Item -Path "$dllZSTD_x64" -Destination "c:\$dllDir\"
    echo "zstd:x64-windows has been successfully built."
}
elseif ($is_zstd_dll_x86_exist)
{
    Move-Item -Path "$dllZSTD_x86" -Destination "c:\$dllDir\"
    echo "zstd:x86-windows has been successfully built."
}
else
{
    echo "Failed to build zstd."
    Exit 1
}

if ($is_rocksdb_dll_x64_exist)
{
    Move-Item -Path "$dllRocksdb_x64" -Destination "c:\$dllDir\"
    Rename-Item -Path "c:\$dllDir\rocksdb-shared.dll" -NewName "$libName.dll"
    $includeDir = "C:\vcpkg\installed\x64-windows\include\rocksdb"
    echo "rocksdb:x64-windows has been successfully built."
}
elseif ($is_rocksdb_dll_x86_exist)
{
    Move-Item -Path "$dllRocksdb_x86" -Destination "c:\$dllDir\"
    Rename-Item -Path "c:\$dllDir\rocksdb-shared.dll" -NewName "$libName.dll"
    $includeDir = "C:\vcpkg\installed\x86-windows\include\rocksdb"
    echo "rocksdb:x86-windows has been successfully built."
}
else
{
    echo "Failed to build rocksdb."
    Exit 1
}

echo "================== Start generate source code for rocksdb =================="
jextract --source --header-class-name "$headerClassName" --output c:\$srcDir -t "$sourcecodePackage" -I $includeDir $includeDir\c.h
$is_src_dir = [System.IO.Directory]::Exists("c:\$srcDir")
if (!$is_src_dir)
{
    echo "Failed to generate code by jextract"
    Exit 1
}

echo "================== Build completed =================="
Start-Sleep -Seconds 5