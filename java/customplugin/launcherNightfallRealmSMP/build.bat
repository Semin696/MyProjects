@echo off
chcp 65001 >nul
title Nightfall Realm SMP Launcher - Build

echo.
echo   ╔═══════════════════════════════════════╗
echo   ║    Nightfall Realm SMP Launcher       ║
echo   ║        Сборка проекта                 ║
echo   ╚═══════════════════════════════════════╝
echo.

if "%JAVA_HOME%"=="" (
    echo [!] JAVA_HOME не задан!
    echo     Убедитесь что Java 17+ установлена.
    echo.
    set /p "JAVA_PATH=Укажите путь к Java (например C:\Program Files\Java\jdk-17): "
    if not exist "%JAVA_PATH%" (
        echo [X] Путь не найден!
        pause
        exit /b 1
    )
    set "JAVA_HOME=%JAVA_PATH%"
)

echo [*] Java: %JAVA_HOME%
echo [*] Сборка проекта...
echo.

"%JAVA_HOME%\bin\javac" -version 2>nul
if %errorlevel% neq 0 (
    echo [X] Java не найдена! Установите JDK 17+.
    pause
    exit /b 1
)

cd app
if exist "mvnw.cmd" (
    call mvnw.cmd clean package -q
) else if exist "mvn.cmd" (
    call mvn.cmd clean package -q
) else (
    echo [!] Maven не найден в системе.
    echo     Установите Maven или используйте IntelliJ IDEA.
    echo.
    echo     Альтернативно, скачайте Maven: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
cd ..

if %errorlevel% equ 0 (
    echo.
    echo   [✓] Сборка успешно завершена!
    echo   [*] JAR файл: app\target\nightfall-launcher-1.0.0.jar
    echo.
    echo   Запустите лаунчер командой: run.bat
) else (
    echo.
    echo   [X] Ошибка сборки!
)
pause
