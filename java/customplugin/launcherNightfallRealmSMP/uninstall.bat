@echo off
chcp 65001 >nul
title Nightfall Realm SMP - Deletion

echo.
echo   Nightfall Realm SMP - Deletion
echo.
echo   This will delete launcher settings.
echo   Your mods in C:\NightfallRealmSMP will NOT be deleted.
echo.
choice /C YN /M "Delete launcher settings?"
if errorlevel 2 exit /b

echo.
echo Deleting...

if exist "%APPDATA%\NightfallRealmSMP\launcher" (
    rmdir /S /Q "%APPDATA%\NightfallRealmSMP\launcher"
    echo [OK] Launcher settings deleted
)

echo.
echo Done. Next time you launch, the installer will show again.
echo.
pause
