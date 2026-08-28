@echo off
title Malice Visuals
cd /d "%~dp0"
if exist "%~dp0MaliceVisuals.exe" (
  start "" "%~dp0MaliceVisuals.exe"
) else (
  start "" wscript.exe "%~dp0MaliceLauncher.vbs"
)
