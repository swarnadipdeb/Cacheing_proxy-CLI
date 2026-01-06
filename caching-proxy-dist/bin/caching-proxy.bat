@echo off
setlocal enabledelayedexpansion

REM Resolve the directory of this script (even when called via PATH)
set SCRIPT_DIR=%~dp0

REM Remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

REM Build path to JAR (..\lib\caching-proxy.jar)
set JAR=%SCRIPT_DIR%\..\lib\caching-proxy.jar

REM Run the JAR and forward all arguments
java -jar "%JAR%" %*
