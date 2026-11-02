@echo off
setlocal
echo [1/3] Compiling Java classes (MexLauncher)...
if exist "build" rmdir /s /q "build"
mkdir "build"
javac -encoding UTF-8 -d build src\com\craftlauncher\*.java MexLauncher.java
if errorlevel 1 (
    echo [ERROR] Failed to compile Java classes.
    exit /b 1
)

echo [2/3] Bundling FontAwesome and packaging MexLauncher.jar...
if not exist "build\com\craftlauncher" mkdir "build\com\craftlauncher"
copy /Y "src\com\craftlauncher\fa-solid-900.ttf" "build\com\craftlauncher\fa-solid-900.ttf" >nul

jar cfe MexLauncher.jar com.craftlauncher.MexLauncher -C build .
copy /Y MexLauncher.jar Launcher.jar >nul

echo [3/3] Compiling native MexLauncher.exe with GitHub Auto-Sync...
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /target:winexe /optimize+ /platform:anycpu /win32icon:app.ico /out:MexLauncher.exe LauncherBootstrap.cs >nul
if errorlevel 1 (
    echo [ERROR] Failed to compile MexLauncher.exe.
    exit /b 1
)

if exist "MinecraftLauncher.exe" del /Q "MinecraftLauncher.exe"

echo.
echo ===================================================
echo  Build finished successfully!
echo  Executable: MexLauncher.exe
echo  Source:     MexLauncher.java
echo ===================================================
endlocal
