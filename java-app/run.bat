@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Install Maven 3.6.3+ and a JDK 8+ first.
  exit /b 1
)
mvn -q package
if errorlevel 1 exit /b 1
java -jar target\idsnap-app-1.0.0.jar
