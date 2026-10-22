@echo off
setlocal
cd /d "%~dp0InventorySystemi"
call mvnw.cmd %*
exit /b %ERRORLEVEL%
