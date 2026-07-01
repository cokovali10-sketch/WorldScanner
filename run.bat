@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" (
    echo WorldScanner quick launcher
    echo.
    echo 1. Interactive mode
    echo 2. Scan world
    echo 3. Find block
    echo 4. Find item
    echo 5. Find entity
    echo 6. Export JSON
    echo 7. Export CSV
    echo 8. Export TXT
    echo 9. Show help
    echo 0. Exit
    set /p choice="Choose an action: "
    if "%choice%"=="1" goto interactive
    if "%choice%"=="2" goto scan
    if "%choice%"=="3" goto findblock
    if "%choice%"=="4" goto finditem
    if "%choice%"=="5" goto findentity
    if "%choice%"=="6" goto exportjson
    if "%choice%"=="7" goto exportcsv
    if "%choice%"=="8" goto exporttxt
    if "%choice%"=="9" goto help
    goto exit
) else (
    call gradlew.bat run --args="%*"
    goto exit
)

:interactive
call gradlew.bat run --args="--interactive"
goto exit

:scan
set /p world="Enter world folder: "
call gradlew.bat run --args="%world% scan --summary"
goto exit

:findblock
set /p world="Enter world folder: "
set /p target="Enter block id: "
call gradlew.bat run --args="%world% find block %target% --limit=10 --summary"
goto exit

:finditem
set /p world="Enter world folder: "
set /p target="Enter item id: "
call gradlew.bat run --args="%world% find item %target% --limit=10 --summary"
goto exit

:findentity
set /p world="Enter world folder: "
set /p target="Enter entity id: "
call gradlew.bat run --args="%world% find entity %target% --limit=10 --summary"
goto exit

:exportjson
set /p world="Enter world folder: "
set /p out="Enter output file: "
call gradlew.bat run --args="%world% export json %out% --summary"
goto exit

:exportcsv
set /p world="Enter world folder: "
set /p out="Enter output file: "
call gradlew.bat run --args="%world% export csv %out% --summary"
goto exit

:exporttxt
set /p world="Enter world folder: "
set /p out="Enter output file: "
call gradlew.bat run --args="%world% export txt %out% --summary"
goto exit

:help
call gradlew.bat run --args="help"
goto exit

:exit
exit /b 0
