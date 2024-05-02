param([string]$srcDir, [string]$dllDir, [string]$libName, [string]$headerClassName, [string]$sourcecodePackage, [string]$build_params)
Write-Output "srcDir: $srcDir"
Write-Output "dllDir: $dllDir"
Write-Output "libName: $libName"
Write-Output "headerClassName: $headerClassName"
Write-Output "sourcecodePackage: $sourcecodePackage"
Write-Output "build_params: $build_params"
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

#根据构建参数获取加速类型
$speedup_type = ""
$build_params = $build_params.Trim()
if ($build_params -like "*openvino*")
{
    $speedup_type = "OpenVINO"
}
elseif ($build_params -like "*cuda*")
{
    $speedup_type = "CUDA"
}

switch ($speedup_type)
{
    "OpenVINO"
    {
        echo "Enable OpenVINO acceleration for whisper.cpp."

        # 下载openvino运行环境
        $openvino_file_name = $build_params.Substring($build_params.LastIndexOf("/") + 1)
        $openvino_dir_name = $openvino_file_name.Substring(0, $openvino_file_name.Length - 4)
        $openvino_url = $build_params
        $openvino_path = "c:/$openvino_dir_name/runtime/cmake"

        Write-Output "openvino_dir_name: $openvino_dir_name"
        Write-Output "openvino_file_name: $openvino_file_name"
        Write-Output "openvino_path: $openvino_path"
        Write-Output "openvino_url: $openvino_url"

        echo "OpenVINO start downloading......"
        $Job = Start-BitsTransfer -Source "$openvino_url" -Destination "c:\" -TransferType Download -Asynchronous
        while (($Job.JobState -eq "Transferring") -or ($Job.JobState -eq "Connecting"))
        {
            echo "downloading......"
            sleep 5;
        } # Poll for status, sleep for 5 seconds, or perform an action.
        Switch ($Job.JobState)
        {
            "Transferred" {
                Complete-BitsTransfer -BitsJob $Job
            }
            "Error" {
                $Job | Format-List
            } # List the errors.
        }

        $download_success = [System.IO.File]::Exists("c:\$openvino_file_name")
        if (!$download_success)
        {
            echo "Failed to download OpenVINO."
            Exit 1
        }

        Expand-Archive "c:\$openvino_file_name" -DestinationPath "c:\"
        Remove-Item -Path "c:\$openvino_file_name" -Force
        echo "OpenVINO download complete."

        # 初始化openvino
        cd .\models\
        python -m venv openvino_conv_env
        openvino_conv_env\Scripts\activate
        python -m pip install --upgrade pip
        pip install -r requirements-openvino.txt
        & "c:\$openvino_dir_name\setupvars.ps1"
        cd ..

        # 添加CMakeList文件，准备构建
        $fileContent = Get-Content -Path CMakeLists.txt
        $newFileContent = $fileContent -replace "# Add path to modules", "list(APPEND CMAKE_PREFIX_PATH `"$openvino_path`")"
        $newFileContent | Set-Content -Path CMakeLists.txt

        cmake -B build -DWHISPER_OPENVINO=1
        cmake --build build -j --config Release

        Rename-Item -Path "build\bin\Release\whisper.dll" -NewName “$libName.dll”
        $build_success = [System.IO.File]::Exists("c:\whisper.cpp\build\bin\Release\$libName.dll")
        if ($build_success)
        {
            copy "build\bin\Release\$libName.dll" "c:\$dllDir"
        }
        else
        {
            echo "Failed to build whispercpp for OpenVINO."
            Exit 1
        }
    }
    "CUDA"
    {

    }
    default
    {
        echo "Enable CPU acceleration for whisper.cpp."
        cmake . --fresh
        msbuild ALL_BUILD.vcxproj /p:Configuration=Release
        Rename-Item -Path bin\Release\whisper.dll -NewName “$libName.dll”
        $build_success = [System.IO.Directory]::Exists("bin\Release\$libName.dll")
        if ($build_success)
        {
            copy bin\Release\$libName.dll c:\$dllDir
        }
        else
        {
            echo "Failed to build whispercpp for CPU."
            Exit 1
        }
    }
}


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