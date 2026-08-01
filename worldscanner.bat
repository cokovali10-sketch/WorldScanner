@echo off
setlocal
cd /d "%~dp0"

rem Fast standalone launcher built by `:cli:installDist`.
set "DIST_BIN=cli\build\install\worldscanner\bin\worldscanner.bat"

if exist "%DIST_BIN%" goto run

if not exist "gradlew.bat" (
    echo [WorldScanner] No prebuilt distribution and no Gradle wrapper found.
    echo [WorldScanner] Build one first:  gradlew.bat :cli:installDist
    exit /b 1
)

echo [WorldScanner] First run detected - building a standalone launcher ...
call gradlew.bat :cli:installDist --console=plain
if errorlevel 1 exit /b 1

:run
call "%DIST_BIN%" %*
exit /b %errorlevel%
