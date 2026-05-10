@echo off
setlocal EnableDelayedExpansion

set BIN_DIR=%~dp0

if "%1"=="" goto start
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="status" goto status
if "%1"=="logs" goto logs
echo Usage: start.bat [start^|stop^|restart^|status^|logs]
goto :eof

:start
echo [INFO] Starting Report Platform...

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found
    goto :eof
)
echo [INFO] Java version:
java -version 2>&1 | findstr "version"

cd /d "%BIN_DIR%"
if not exist "..\logs" mkdir "..\logs"

echo [INFO] Starting service...
start /b cmd /c java -Xms2g -Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8 -jar "%BIN_DIR%report-backend.jar" >> "..\logs\stdout.log" 2>&1

timeout /t 4 /nobreak >nul
echo [INFO] Service started
echo [INFO] Check logs: start.bat logs
goto :eof

:stop
echo [INFO] Stopping service...
if exist "%BIN_DIR%app.pid" (
    set /p PID=<"%BIN_DIR%app.pid"
    taskkill /PID %PID% /F >nul 2>&1
    del "%BIN_DIR%app.pid" >nul 2>&1
)
echo [INFO] Service stopped
goto :eof

:restart
call :stop
timeout /t 2 /nobreak >nul
call :start
goto :eof

:status
if exist "%BIN_DIR%app.pid" (
    set /p PID=<"%BIN_DIR%app.pid"
    tasklist /FI "PID eq !PID!" 2>nul | find "!PID!" >nul
    if not errorlevel 1 (
        echo Service running (PID: !PID!)
        goto :eof
    )
)
echo Service not running
goto :eof

:logs
if exist "..\logs\stdout.log" (
    powershell -Command "Get-Content '..\logs\stdout.log' -Tail 30"
) else (
    echo Log file not found
)
goto :eof
