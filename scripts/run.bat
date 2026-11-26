@echo off
set PROJECT_DIR=%~dp0..
cd /d "%PROJECT_DIR%"

set CP=bin;lib\spring-core.jar;lib\spring-beans.jar;lib\spring-context.jar;lib\spring-expression.jar;lib\spring-aop.jar;lib\spring-jcl.jar;lib\h2.jar;lib\mysql-connector-j.jar

echo ========================================================
echo Starting Smart Parking Allocation System...
echo ========================================================
set JAVA_CMD=java
if exist "C:\Program Files\Java\jdk-26.0.2\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
) else if defined JAVA_HOME (
    set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
)
%JAVA_CMD% -Dspring.classformat.ignore=true -cp "%CP%" com.smartparking.MainApp %*
