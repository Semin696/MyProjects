@echo off
chcp 65001 >nul
title Nightfall Realm SMP Launcher

if not exist "app\pom.xml" (
    echo Run this script from the project folder
    pause
    exit /b 1
)

if not exist "app\target\classes\com\nightfallrealm\Launcher.class" (
    echo Building...
    call app\mvnw.cmd clean package -q
    if errorlevel 1 (
        echo Build failed
        pause
        exit /b 1
    )
)

echo Starting launcher...

if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\java\jdk-17.0.19"
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java not found. Install JDK 17+.
    pause
    exit /b 1
)

set "JFX=%USERPROFILE%\.m2\repository\org\openjfx"
set "BASE=%JFX%\javafx-base\17.0.9\javafx-base-17.0.9.jar;%JFX%\javafx-base\17.0.9\javafx-base-17.0.9-win.jar"
set "GFX=%JFX%\javafx-graphics\17.0.9\javafx-graphics-17.0.9.jar;%JFX%\javafx-graphics\17.0.9\javafx-graphics-17.0.9-win.jar"
set "CTRL=%JFX%\javafx-controls\17.0.9\javafx-controls-17.0.9.jar;%JFX%\javafx-controls\17.0.9\javafx-controls-17.0.9-win.jar"
set "MP=%BASE%;%GFX%;%CTRL%"
set "CP=app\target\classes;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"

"%JAVA_HOME%\bin\java.exe" ^
    --module-path "%MP%" ^
    --add-modules javafx.controls,javafx.graphics ^
    -cp "%CP%" ^
    com.nightfallrealm.Launcher

if errorlevel 1 (
    echo Launch failed. Check that Java 17+ is installed.
    pause
)
