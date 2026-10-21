@echo off
cd /d "%~dp0"
java -jar "ExtratorSPED-v0.1.6.jar"
if errorlevel 1 pause
