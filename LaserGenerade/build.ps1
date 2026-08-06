$ErrorActionPreference = "Stop"

$mingwBin = "C:\Users\SystemX\AppData\Local\Microsoft\WinGet\Packages\BrechtSanders.WinLibs.POSIX.UCRT_Microsoft.Winget.Source_8wekyb3d8bbwe\mingw64\bin"
$gpp = Join-Path $mingwBin "g++.exe"
$windres = Join-Path $mingwBin "windres.exe"

& $windres "app.rc" -O coff -o "app.res"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $gpp -std=c++17 -O2 -municode -mwindows -specs=no-default-manifest -static -static-libgcc -static-libstdc++ "main.cpp" "app.res" -o "PhotoToBW.exe" `
    -lgdiplus -lcomdlg32 -lole32 -luuid -luser32 -lgdi32 -lcomctl32 -lshell32
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "OK: PhotoToBW.exe собран"
