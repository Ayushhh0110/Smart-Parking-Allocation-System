@echo off
echo ========================================================
echo Compiling Smart Parking Allocation System (Java 17+)
echo ========================================================

set PROJECT_DIR=%~dp0..
cd /d "%PROJECT_DIR%"

if not exist bin mkdir bin

set CP=lib\spring-core.jar;lib\spring-beans.jar;lib\spring-context.jar;lib\spring-expression.jar;lib\spring-aop.jar;lib\spring-jcl.jar;lib\h2.jar;lib\mysql-connector-j.jar

echo Finding all Java source files...
dir /s /b src\main\java\*.java > sources.txt

echo Running javac compiler with -release 17...
set JAVAC_CMD=javac
if exist "C:\Program Files\Java\jdk-26.0.2\bin\javac.exe" (
    set JAVAC_CMD="C:\Program Files\Java\jdk-26.0.2\bin\javac.exe"
) else if defined JAVA_HOME (
    set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
)
%JAVAC_CMD% --release 17 -encoding UTF-8 -cp "%CP%" -d bin @sources.txt

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation completed successfully into bin/
    del sources.txt
) else (
    echo [ERROR] Compilation failed with error code %ERRORLEVEL%
    del sources.txt
    exit /b %ERRORLEVEL%
)

echo Copying resources to bin...
xcopy /s /y /q src\main\resources\* bin\ >nul 2>&1
echo Done.
