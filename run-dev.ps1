# =========================================================================
# Library Management System - PowerShell Dev Server Launcher
# =========================================================================

$env:JAVA_HOME = "C:\Users\User\.tools\jdk-17.0.10+7"
$env:CATALINA_HOME = "C:\Users\User\.tools\apache-tomcat-10.1.20"
$env:Path = "C:\Users\User\.tools\jdk-17.0.10+7\bin;C:\Users\User\.tools\apache-maven-3.9.6\bin;C:\Users\User\.tools\pgsql\bin;" + $env:Path

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  Enterprise Library Management System (J2EE & PostgreSQL)" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  Web Interface: http://localhost:8080/" -ForegroundColor Yellow
Write-Host "  Admin Login:   admin / Admin@123" -ForegroundColor Yellow
Write-Host "  Librarian:     librarian / Librarian@123" -ForegroundColor Yellow
Write-Host "  Member:        johndoe / Member@123" -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan

# Check if PostgreSQL is up
try {
    & pg_isready -h 127.0.0.1 -p 5432 -U library_app -d library_db
} catch {
    Write-Host "Starting background PostgreSQL daemon..." -ForegroundColor Magenta
    Start-Process -FilePath "C:\Users\User\.tools\pgsql\bin\postgres.exe" -ArgumentList "-D", "C:\Users\User\.tools\pgsql_data" -WindowStyle Hidden
    Start-Sleep -Seconds 3
}

# Run Embedded Tomcat Server via Maven
mvn compile exec:java "-Dexec.mainClass=com.library.Main"
