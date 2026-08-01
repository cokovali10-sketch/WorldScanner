@echo off
setlocal
cd /d "%~dp0"

if /i "%~1"=="gui" (
    call gui.bat
    exit /b %errorlevel%
)

if "%~1"=="" (
    echo WorldScanner 2.0 - Minecraft Anvil world item scanner
    echo.
    echo Usage:  run.bat ^<command^> [args]
    echo.
    echo Commands:
    echo   run.bat gui                             Open the desktop GUI
    echo   run.bat info  ^<world-path^>            Inspect a world
    echo   run.bat stats ^<world-path^>            Item / block-entity / entity statistics
    echo   run.bat find  ^<world-path^> --item ^<id^> [options]
    echo   run.bat --version                      Print the tool version
    echo   run.bat help                           Show this help
    echo.
    echo Find options:
    echo   --item ^<id^>          repeatable, e.g. --item diamond
    echo   --items=a,b,c          comma-separated list
    echo   --dimension=overworld^|nether^|end
    echo   --region=rx,rz         limit to one region file
    echo   --limit=^<N^>            stop after N results
    echo   --threads=^<N^>          worker threads
    echo   --json=^<file^>         export to JSON
    echo   --csv=^<file^>          export to CSV
    echo   --summary              compact output
    echo   --color ^| --no-color  force ANSI colors
    echo.
    echo Examples:
    echo   run.bat find C:/worlds/survival --item diamond --summary
    echo   run.bat find C:/worlds/survival --items=shulker_box,bundle --limit=50
    echo   run.bat stats C:/worlds/survival
    exit /b 0
)

call worldscanner.bat %*
exit /b %errorlevel%
