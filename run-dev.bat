@echo off
REM =========================================================================
REM Library Management System - Development Server Launcher
REM =========================================================================

echo Starting Library Management System Development Server...

set "JAVA_HOME=C:\Users\User\.tools\jdk-17.0.10+7"
set "CATALINA_HOME=C:\Users\User\.tools\apache-tomcat-10.1.20"
set "Path=C:\Users\User\.tools\jdk-17.0.10+7\bin;C:\Users\User\.tools\apache-maven-3.9.6\bin;C:\Users\User\.tools\pgsql\bin;%Path%"

echo Checking PostgreSQL...
pg_isready -h 127.0.0.1 -p 5432 -U library_app -d library_db
if %ERRORLEVEL% NEQ 0 (
    echo Starting PostgreSQL service...
    start "" "C:\Users\User\.tools\pgsql\bin\postgres.exe" -D "C:\Users\User\.tools\pgsql_data"
    timeout /t 3 /nobreak >nul
)

echo.
echo Launching Tomcat Application Server on port 8080...
echo Access Web UI at: http://localhost:8080/
echo Default Admin: admin / Admin@123
echo.

call mvn compile exec:java "-Dexec.mainClass=com.library.Main"
pause
