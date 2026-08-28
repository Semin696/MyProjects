@echo off
setlocal EnableDelayedExpansion
title Malice Visuals - Launcher
cd /d "%~dp0"

:: Auto-detect Java 21+ / JAVA_HOME
set "JAVA_FOUND=0"
set "JAVA_HOME="

if exist "%USERPROFILE%\.jdks\ms-21.0.11\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    set "JAVA_FOUND=1"
    goto javaReady
)
if exist "%USERPROFILE%\.jdks\ms-21.0.10\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.10"
    set "JAVA_FOUND=1"
    goto javaReady
)
if exist "%USERPROFILE%\.jdks\openjdk-26\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-26"
    set "JAVA_FOUND=1"
    goto javaReady
)
if exist "%USERPROFILE%\.jdks\ms-25.0.2\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\ms-25.0.2"
    set "JAVA_FOUND=1"
    goto javaReady
)
if exist "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
    set "JAVA_FOUND=1"
    goto javaReady
)
if exist "C:\Program Files\Java\jdk-24\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    set "JAVA_FOUND=1"
    goto javaReady
)

for /d %%i in ("%USERPROFILE%\.jdks\*") do (
    if exist "%%~fi\bin\java.exe" if exist "%%~fi\release" (
        findstr /C:"JAVA_VERSION=\"21" /C:"JAVA_VERSION=\"2" "%%~fi\release" >nul 2>&1
        if not errorlevel 1 (
            echo %%~ni | findstr /I "17 1.8 8u jre11 jdk-11" >nul
            if errorlevel 1 (
                set "JAVA_HOME=%%~fi"
                set "JAVA_FOUND=1"
                goto javaReady
            )
        )
    )
)

:javaReady

if "!JAVA_FOUND!"=="1" (
    if defined JAVA_HOME (
        set "PATH=!JAVA_HOME!\bin;!PATH!"
    )
)

if not "%~1"=="" (
    if "!JAVA_FOUND!"=="0" goto noJava
    call gradlew.bat %*
    goto end
)

:menu
cls
echo ====================================================
echo                Malice Visuals Launcher
echo ====================================================
echo.
if "!JAVA_FOUND!"=="1" (
    if defined JAVA_HOME (
        echo  [Java: !JAVA_HOME!]
    ) else (
        echo  [Java: System PATH]
    )
) else (
    echo  [WARNING: Java JDK was NOT detected!]
)
echo.
echo  [0] Open Malice Visuals Launcher
echo  [1] Run Client (runClient)
echo  [2] Build JAR  (build)
echo  [3] Clean and Run (clean runClient)
echo  [4] Generate Loom Sources (genSources)
echo  [5] Exit
echo.
echo ====================================================
set "choice=0"
set /p choice="Select option [0-5] (default 0): "

if "!choice!"=="0" goto runGui
if "!choice!"=="1" goto runClient
if "!choice!"=="2" goto buildProject
if "!choice!"=="3" goto cleanRun
if "!choice!"=="4" goto genSources
if "!choice!"=="5" goto exitProgram

echo Invalid choice.
timeout /t 2 > nul
goto menu

:runGui
echo.
echo [INFO] Launching Malice Visuals...
if exist "%~dp0MaliceVisuals.exe" (
  start "" "%~dp0MaliceVisuals.exe"
) else (
  start "" wscript.exe "%~dp0MaliceLauncher.vbs"
)
goto exitProgram

:checkJava
if "!JAVA_FOUND!"=="0" goto noJava
goto :eof

:noJava
echo.
echo ====================================================
echo [ERROR] Java JDK 21+ is required to build/run!
echo.
echo Please install JDK 21:
echo https://adoptium.net/temurin/releases/?version=21
echo.
echo Or set JAVA_HOME in your Windows Environment Variables.
echo ====================================================
echo.
pause
goto menu

:runClient
call :checkJava
if "!JAVA_FOUND!"=="0" goto menu
echo.
echo [INFO] Starting Minecraft Client (runClient)...
echo.
call gradlew.bat runClient
goto end

:buildProject
call :checkJava
if "!JAVA_FOUND!"=="0" goto menu
echo.
echo [INFO] Building project (build)...
echo.
call gradlew.bat build
goto end

:cleanRun
call :checkJava
if "!JAVA_FOUND!"=="0" goto menu
echo.
echo [INFO] Cleaning and starting client (clean runClient)...
echo.
call gradlew.bat clean runClient
goto end

:genSources
call :checkJava
if "!JAVA_FOUND!"=="0" goto menu
echo.
echo [INFO] Generating Loom sources (genSources)...
echo.
call gradlew.bat genSources
goto end

:end
echo.
echo [INFO] Finished.
pause
goto menu

:exitProgram
exit /b 0
