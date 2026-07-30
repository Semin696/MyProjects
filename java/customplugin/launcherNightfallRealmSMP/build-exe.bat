@echo off
chcp 65001 >nul
title Nightfall Realm SMP - Build EXE

echo.
echo   Nightfall Realm SMP - Build EXE
echo.

echo [*] Building JAR...
cd app
call mvnw.cmd compile package -q -DskipTests
if errorlevel 1 (
    echo Retrying...
    call mvnw.cmd package -q -DskipTests
    if errorlevel 1 (
        echo [X] JAR build failed
        pause
        exit /b 1
    )
)
cd ..
echo [OK] JAR built

echo [*] Copying NightfallMenu mod source to jpackage input...
xcopy /E /I /Y /Q app\mod-nightfallmenu app\target\mod-nightfallmenu >nul
if errorlevel 1 (
    echo [X] Failed to copy mod source
    pause
    exit /b 1
)
echo [OK] Mod source copied

echo [*] Downloading JavaFX JMODs...
set "JFX_JMODS=%USERPROFILE%\.m2\jmods"
if not exist "%JFX_JMODS%\javafx.controls.jmod" (
    if not exist "%TEMP%\javafx-jmods.zip" (
        powershell -Command "$wc = New-Object System.Net.WebClient; $wc.Headers.Add('user-agent','Mozilla/5.0'); Write-Host 'Downloading...'; $wc.DownloadFile('https://download2.gluonhq.com/openjfx/17.0.9/openjfx-17.0.9_windows-x64_bin-jmods.zip', '%TEMP%\javafx-jmods.zip')"
    )
    if not exist "%TEMP%\javafx-jmods" mkdir "%TEMP%\javafx-jmods"
    powershell -Command "Expand-Archive -Path '%TEMP%\javafx-jmods.zip' -DestinationPath '%TEMP%\javafx-jmods' -Force"
    if exist "%TEMP%\javafx-jmods\javafx-jmods-17.0.9" (
        mkdir "%JFX_JMODS%" 2>nul
        copy "%TEMP%\javafx-jmods\javafx-jmods-17.0.9\*" "%JFX_JMODS%\" >nul
    ) else (
        mkdir "%JFX_JMODS%" 2>nul
        copy "%TEMP%\javafx-jmods\*" "%JFX_JMODS%\" >nul
    )
    echo [OK] JMODs downloaded
)

set "ICON_PNG=app\src\main\resources\images\icon.png"
set "ICON_ICO=app\src\main\resources\images\icon.ico"

if not exist "%ICON_ICO%" (
    echo [*] Converting icon PNG to ICO...
    powershell -NoProfile -ExecutionPolicy Bypass -File convert-icon.ps1 -pngPath "%ICON_PNG%" -icoPath "%ICON_ICO%"
    if errorlevel 1 (
        echo [X] Icon conversion failed
        pause
        exit /b 1
    )
)

echo [*] Creating app image (portable, no WiX needed)...
jpackage --type app-image ^
    --name "Nightfall Realm SMP" ^
    --app-version 1.0.0 ^
    --vendor Nightfall ^
    --input app\target ^
    --main-jar nightfall-launcher-1.0.0.jar ^
    --main-class com.nightfallrealm.Launcher ^
    --module-path "%JFX_JMODS%" ^
    --add-modules javafx.controls,javafx.graphics ^
    --dest dist ^
    --icon "%ICON_ICO%"

if errorlevel 1 (
    echo [X] JMOD app-image failed. Trying with classpath jars...
    set "JFX=%USERPROFILE%\.m2\repository\org\openjfx"
    jpackage --type app-image ^
        --name "Nightfall Realm SMP" ^
        --app-version 1.0.0 ^
        --vendor Nightfall ^
        --input app\target ^
        --main-jar nightfall-launcher-1.0.0.jar ^
        --main-class com.nightfallrealm.Launcher ^
        --module-path "%JFX%\javafx-base\17.0.9\javafx-base-17.0.9.jar;%JFX%\javafx-base\17.0.9\javafx-base-17.0.9-win.jar;%JFX%\javafx-graphics\17.0.9\javafx-graphics-17.0.9.jar;%JFX%\javafx-graphics\17.0.9\javafx-graphics-17.0.9-win.jar;%JFX%\javafx-controls\17.0.9\javafx-controls-17.0.9.jar;%JFX%\javafx-controls\17.0.9\javafx-controls-17.0.9-win.jar" ^
        --add-modules javafx.controls,javafx.graphics ^
        --dest dist ^
        --icon "%ICON_ICO%"
    if errorlevel 1 (
        echo [X] Build failed. Make sure JDK 17+ with jpackage is installed.
        pause
        exit /b 1
    )
)

echo [OK] App image created: dist\Nightfall Realm SMP\

where light.exe >nul 2>nul
if errorlevel 1 (
    echo [!] WiX not found. App image is ready, but no EXE installer created.
    echo     Install WiX Toolset from: https://wixtoolset.org
    echo     Then run: jpackage --type exe --app-image "dist\Nightfall Realm SMP" --dest dist --name "Nightfall Realm SMP" --win-dir-chooser --win-menu --win-shortcut
) else (
    echo [*] Creating EXE installer...
    jpackage --type exe ^
        --app-image "dist\Nightfall Realm SMP" ^
        --dest dist ^
        --name "Nightfall Realm SMP" ^
        --icon "%ICON_ICO%" ^
        --win-dir-chooser ^
        --win-menu ^
        --win-shortcut
    if errorlevel 1 (
        echo [X] EXE creation failed, but app image is ready.
    ) else (
        echo [OK] EXE created: dist\Nightfall Realm SMP.exe
    )
)

echo.
echo [OK] Done. Files in dist\ folder.
pause
