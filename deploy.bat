@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "SERVER_PLUGINS=c:\Users\Alli computer\OneDrive\Desktop\Server\plugins"
set "JAR_NAME=truecombatmanager-1.2.1.jar"
set "SOURCE_JAR=%CD%\target\%JAR_NAME%"
set "DEST_JAR=%SERVER_PLUGINS%\%JAR_NAME%"

if not exist "%SOURCE_JAR%" (
    echo ERROR: Jar not found: %SOURCE_JAR%
    exit /b 1
)

if not exist "%SERVER_PLUGINS%\" (
    echo ERROR: Server plugins folder not found: %SERVER_PLUGINS%
    exit /b 1
)

if exist "%DEST_JAR%" (
    for /f "tokens=1-4 delims=/ " %%a in ("%DATE%") do set "D=%%d%%b%%c"
    for /f "tokens=1-2 delims=:." %%a in ("%TIME: =0%") do set "T=%%a%%b"
    set "BACKUP_JAR=%DEST_JAR%.%D%_%T%.bak"
    echo Backing up to: %BACKUP_JAR%
    copy /Y "%DEST_JAR%" "%BACKUP_JAR%" >nul
    if errorlevel 1 (
        echo ERROR: Backup failed. Server may have the file locked.
        exit /b 1
    )
)

copy /Y "%SOURCE_JAR%" "%DEST_JAR%"
if errorlevel 1 (
    echo Copy failed.
    exit /b 1
)

echo OK: %DEST_JAR%
endlocal & exit /b 0
