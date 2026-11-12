@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
  set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
)

echo ==========================================
echo  BaoJingTong - build and start
echo ==========================================
echo [1/2] Building (first run downloads dependencies)...
call "%MVN_CMD%" -q clean package -DskipTests
if errorlevel 1 (
  echo BUILD FAILED. See output above.
  pause
  exit /b 1
)

echo [2/2] Starting... Console: http://localhost:8091
echo        H2 DB console: http://localhost:8091/h2-console
echo        Swagger UI   : http://localhost:8091/swagger-ui.html
echo        Press Ctrl+C to stop.
echo ==========================================
java -jar bootstrap\target\baoxian.jar
pause
