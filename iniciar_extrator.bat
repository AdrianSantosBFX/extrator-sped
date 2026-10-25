@echo off
cd /d "%~dp0"
java -jar "ExtratorSPED-v0.1.11.jar"
if errorlevel 1 pause
