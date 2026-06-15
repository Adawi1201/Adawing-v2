@echo off
if "%BUILD_LAUNCHED%"=="1" goto main
set BUILD_LAUNCHED=1
start "AdaWing Build" cmd /k "%~f0"
exit

:main
setlocal

set PROJECT_DIR=%~dp0..
set BACKEND_DIR=%PROJECT_DIR%\Blog
set FRONTEND_DIR=%PROJECT_DIR%\adawing-ui
set DEPLOY_DIR=%PROJECT_DIR%\deploy
set DIST_DIR=%DEPLOY_DIR%\adawing-v2

echo.
echo =========================================
echo   AdaWing v2 - Build ^& Package
echo =========================================
echo   Project: %PROJECT_DIR%
echo.

:: ---- Step 1: Check tools ----
echo [1/4] Checking build tools...
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo   [FAIL] java not found - Install JDK 21
    goto fail
)
echo   [OK] Java

where mvn >nul 2>nul
if errorlevel 1 (
    echo   [FAIL] mvn not found - Install Maven 3.9+
    goto fail
)
echo   [OK] Maven

where node >nul 2>nul
if errorlevel 1 (
    echo   [FAIL] node not found - Install Node.js 18+
    goto fail
)
echo   [OK] Node

where pnpm >nul 2>nul
if errorlevel 1 (
    echo   [FAIL] pnpm not found - Run: npm install -g pnpm
    goto fail
)
echo   [OK] pnpm

:: ---- Step 2: Backend ----
echo.
echo [2/4] Building backend (Maven)...

cd /d "%BACKEND_DIR%"
call mvn clean package -DskipTests -q
if errorlevel 1 (
    cd /d "%PROJECT_DIR%"
    goto fail
)

if not exist "%BACKEND_DIR%\blog-boot\target\blog-boot-1.0-SNAPSHOT.jar" (
    echo   [FAIL] jar not found
    cd /d "%PROJECT_DIR%"
    goto fail
)
echo   [OK] Backend built

:: ---- Step 3: Frontend ----
echo.
echo [3/4] Building frontend (Vite)...

cd /d "%FRONTEND_DIR%"
call pnpm install
if errorlevel 1 (
    cd /d "%PROJECT_DIR%"
    goto fail
)

call pnpm run build
if errorlevel 1 (
    cd /d "%PROJECT_DIR%"
    goto fail
)

if not exist "%FRONTEND_DIR%\dist\index.html" (
    echo   [FAIL] dist not found
    cd /d "%PROJECT_DIR%"
    goto fail
)
echo   [OK] Frontend built

:: ---- Step 4: Collect files ----
echo.
echo [4/4] Collecting files...

if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%" 2>nul
mkdir "%DIST_DIR%"

:: backend jar
copy /y "%BACKEND_DIR%\blog-boot\target\blog-boot-1.0-SNAPSHOT.jar" "%DIST_DIR%\adawing-backend.jar"
if errorlevel 1 echo   [WARN] jar copy failed

:: frontend
mkdir "%DIST_DIR%\frontend"
xcopy /e /y /i "%FRONTEND_DIR%\dist" "%DIST_DIR%\frontend" >nul
if errorlevel 1 echo   [WARN] frontend copy failed

:: config files
copy /y "%BACKEND_DIR%\blog-boot\src\main\resources\db\schema.sql" "%DIST_DIR%" >nul
copy /y "%DEPLOY_DIR%\application-prod.yaml" "%DIST_DIR%" >nul
copy /y "%DEPLOY_DIR%\setup.sh"              "%DIST_DIR%" >nul
copy /y "%DEPLOY_DIR%\run.sh"                "%DIST_DIR%" >nul
copy /y "%DEPLOY_DIR%\nginx.conf"            "%DIST_DIR%" >nul

:: verify
echo   Checking collected files:
dir "%DIST_DIR%" /s /b 2>nul
echo.
echo   [OK] Files collected

:: ---- Done ----
echo.
echo =========================================
echo   BUILD SUCCESS
echo =========================================
echo.
echo   Output: deploy\adawing-v2\
dir "%DIST_DIR%" /s /b 2>nul
echo.
echo   Next step:
echo     1. tar -czf adawing-v2.tar.gz adawing-v2\
echo        ^(or use 7-Zip / WinRAR to compress^)
echo     2. scp adawing-v2.tar.gz root@YOUR_IP:/opt/
echo     3. ssh root@YOUR_IP
echo        cd /opt ^&^& tar xzf adawing-v2.tar.gz
echo        cd adawing-v2 ^&^& sudo bash setup.sh
echo.
echo =========================================
echo   Type exit to close this window.
echo.

endlocal
goto :eof

:fail
echo.
echo =========================================
echo   BUILD FAILED
echo =========================================
echo.
echo   Check the error messages above.
echo   Type exit to close this window.
echo.
endlocal
echo   Type exit to close this window.
echo.

endlocal
goto :eof

:fail
echo.
echo =========================================
echo   BUILD FAILED
echo =========================================
echo.
echo   Check the error messages above.
echo   Type exit to close this window.
echo.
endlocal
