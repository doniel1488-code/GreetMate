@echo off
gradle --no-daemon clean build
if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)
echo Build finished. See build\libs
pause
