@echo off
setlocal enabledelayedexpansion

set "MVNW_REPOURL=https://repo.maven.apache.org/maven2"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [*] Downloading Maven %MAVEN_VERSION%...
    if not exist "%USERPROFILE%\.m2\wrapper\dists" mkdir "%USERPROFILE%\.m2\wrapper\dists"
    set "MAVEN_URL=%MVNW_REPOURL%/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
    set "ZIP_FILE=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
    powershell -Command "$wc = New-Object System.Net.WebClient; $wc.Headers.Add('user-agent','Mozilla/5.0'); try { $wc.DownloadFile('%MAVEN_URL%', '%ZIP_FILE%'); Write-Host '[OK] Downloaded' } catch { Write-Host '[ERROR] Failed to download Maven'; exit 1 }"
    if not exist "!ZIP_FILE!" (
        echo [ERROR] Download failed
        exit /b 1
    )
    powershell -Command "try { Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\' -Force; Write-Host '[OK] Extracted' } catch { Write-Host '[ERROR] Extract failed: $_'; exit 1 }"
    del "%ZIP_FILE%"
    if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
        echo [ERROR] Maven extraction failed
        exit /b 1
    )
    echo [*] Maven ready
)

set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
set "MVNW_MODDIR=%USERPROFILE%\.m2\repository"

"%MVN_CMD%" %*
