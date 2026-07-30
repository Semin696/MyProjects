@echo off
chcp 65001 >nul
title Nightfall Realm SMP - Установка

echo.
echo   ╔══════════════════════════════════════════════════╗
echo   ║        Nightfall Realm SMP - Установка          ║
echo   ╚══════════════════════════════════════════════════╝
echo.

echo [*] Создание папок...
if not exist "C:\NightfallRealmSMP\mods" mkdir "C:\NightfallRealmSMP\mods"
if not exist "C:\NightfallRealmSMP\resourcepacks" mkdir "C:\NightfallRealmSMP\resourcepacks"
if not exist "%APPDATA%\NightfallRealmSMP\launcher" mkdir "%APPDATA%\NightfallRealmSMP\launcher"
if not exist "%APPDATA%\NightfallRealmSMP\mods" mkdir "%APPDATA%\NightfallRealmSMP\mods"
if not exist "%APPDATA%\NightfallRealmSMP\resourcepacks" mkdir "%APPDATA%\NightfallRealmSMP\resourcepacks"

echo [*] Папки созданы успешно.
echo.
echo   [✓] C:\NightfallRealmSMP\mods
echo   [✓] C:\NightfallRealmSMP\resourcepacks
echo   [✓] %%APPDATA%%\NightfallRealmSMP
echo.
echo [*] Лаунчер готов к использованию!
echo.
echo  Для запуска используйте: run.bat
echo  Или откройте проект в IntelliJ IDEA.
echo.
pause
