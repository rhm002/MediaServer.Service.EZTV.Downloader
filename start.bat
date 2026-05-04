@echo off
REM MediaServer.Service.EZTV.Downloader - Windows Start Script
REM Usage: start.bat [profile]
REM Profile order: command-line arg > .env SPRING_PROFILES_ACTIVE > psql-sphere

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%target\MediaServer.Service.EZTV.Downloader.jar"
set "ENV_FILE=%SCRIPT_DIR%.env"

REM Load .env file
if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims== eol=#" %%A in ("%ENV_FILE%") do (
        set "%%A=%%B"
    )
) else (
    echo WARN: .env not found at %ENV_FILE%
)

REM Command-line arg overrides .env
if not "%~1"=="" set "SPRING_PROFILES_ACTIVE=%~1"
if "%SPRING_PROFILES_ACTIVE%"=="" set "SPRING_PROFILES_ACTIVE=psql-sphere"

if not exist "%JAR%" (
    echo ERROR: JAR not found: %JAR%
    echo Run: mvn clean package -DskipTests
    exit /b 1
)

REM Kill any previous Java instance running this JAR
for /f "tokens=1" %%P in ('wmic process where "Name='java.exe' and CommandLine like '%%MediaServer.Service.EZTV.Downloader%%'" get ProcessId 2^>nul ^| findstr /r "[0-9]"') do (
    echo Stopping previous instance [PID: %%P]
    taskkill /PID %%P /F >nul 2>&1
)

echo Starting MediaServer.Service.EZTV.Downloader [profile: %SPRING_PROFILES_ACTIVE%]
start "MediaServer.Service.EZTV.Downloader" java -jar "%JAR%" --spring.profiles.active=%SPRING_PROFILES_ACTIVE%

endlocal
