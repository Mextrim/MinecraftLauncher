@echo off
setlocal
echo [1/3] Compiling Java classes...
if exist "build" rmdir /s /q "build"
mkdir "build"
javac -encoding UTF-8 -d build src\com\craftlauncher\*.java
if errorlevel 1 (
    echo [ERROR] Failed to compile Java classes.
    pause
    exit /b 1
)

echo [2/3] Bundling FontAwesome and packaging Launcher.jar...
if not exist "build\com\craftlauncher" mkdir "build\com\craftlauncher"
copy /Y "src\com\craftlauncher\fa-solid-900.ttf" "build\com\craftlauncher\fa-solid-900.ttf" >nul

jar cfe Launcher.jar com.craftlauncher.Main -C build .
if exist "dist\MinecraftLauncher\app" (
    copy /Y Launcher.jar "dist\MinecraftLauncher\app\Launcher.jar" >nul
)

echo [3/3] Compiling native MinecraftLauncher.exe...
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /target:winexe /optimize+ /platform:anycpu /win32icon:app.ico /out:MinecraftLauncher.exe LauncherBootstrap.cs >nul

echo.
echo ===================================================
echo  Build finished successfully with FontAwesome!
echo  Run: MinecraftLauncher.exe or Launcher.jar
echo ===================================================
endlocal
