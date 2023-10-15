set dllFile_x64="C:\vcpkg\installed\x64-windows\bin\xxhash.dll"
set dllFile_x86="C:\vcpkg\installed\x86-windows\bin\xxhash.dll"
set includeDir=""
set srcDir=%1
set dllDir=%2
set libName=%3
echo %srcDir% %dllDir% %libName%

echo "Start to build xxHash ...... "
vcpkg install xxhash

if exist "%dllFile_x64%" (
   move %dllFile_x64% c:\%dllDir%\
   rename c:\%dllDir%\xxhash.dll %libName%.dll
   set includeDir="C:\vcpkg\installed\x64-windows\include"
   echo "xxhash:x64-windows has been successfully built."
) else if exist %dllFile_x86% (
   move %dllFile_x86% c:\%dllDir%\
   rename c:\%dllDir%\xxhash.dll %libName%.dll
   set includeDir="C:\vcpkg\installed\x86-windows\include"
   echo "xxhash:x86-windows has been successfully built."
) else (
  echo "Failed to build xxhash."
  exit 1
)

echo "================== Start generate source code for xxHash =================="
jextract --source --header-class-name xxHash --output c:\%srcDir% -t com.silong.foundation.utilities.xxhash.generated -I %includeDir% %includeDir%\xxhash.h
if not exist c:\%srcDir% (
  echo "Failed to generate code by jextract"
  exit 1
)

echo "================== Build completed =================="
timeout 10s


