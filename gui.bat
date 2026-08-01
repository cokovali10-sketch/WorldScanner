@echo off
setlocal
cd /d "%~dp0"

rem Desktop GUI launcher.
rem First run downloads Compose dependencies and takes a while; later runs are fast.
call gradlew.bat :ui:run --console=plain
exit /b %errorlevel%
