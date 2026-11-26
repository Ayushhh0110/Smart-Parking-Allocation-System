@echo off
set PROJECT_DIR=%~dp0..
cd /d "%PROJECT_DIR%"

set CP=bin;lib\spring-core.jar;lib\spring-beans.jar;lib\spring-context.jar;lib\spring-expression.jar;lib\spring-aop.jar;lib\spring-jcl.jar;lib\h2.jar;lib\mysql-connector-j.jar

echo ========================================================
echo Starting Smart Parking Web Dashboard on Port 8080...
echo ========================================================
echo Opening browser at http://localhost:8080 in 2 seconds...
start "" timeout /t 2 /nobreak >nul & start http://localhost:8080

"C:\Program Files\Java\jdk-26.0.2\bin\java.exe" -Dspring.classformat.ignore=true -cp "%CP%" com.smartparking.MainApp --web
