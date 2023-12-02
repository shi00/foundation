param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"
echo $env:path
echo $env:UserName

$dllFile_x64 = "C:\vcpkg\installed\x64-windows\bin\rocksdb-shared.dll"
$dllFile_x86 = "C:\vcpkg\installed\x86-windows\bin\rocksdb-shared.dll"
$includeDir = ""

echo "Start to build rocksdb ...... "
.\vcpkg install rocksdb

$is_dll_x86_exist = [System.IO.File]::Exists($dllFile_x86)
$is_dll_x64_exist = [System.IO.File]::Exists($dllFile_x64)

if ($is_dll_x64_exist)
{
    Move-Item -Path "$dllFile_x64" -Destination "c:\$dllDir\"
    Rename-Item -Path "c:\$dllDir\rocksdb-shared.dll" -NewName "$libName.dll"
    $includeDir = "C:\vcpkg\installed\x64-windows\include\rocksdb"
    echo "rocksdb:x64-windows has been successfully built."
}
elseif ($is_dll_x86_exist)
{
    Move-Item -Path "$dllFile_x86" -Destination "c:\$dllDir\"
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