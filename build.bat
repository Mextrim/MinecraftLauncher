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

echo [2/3] Packaging Launcher.jar...
jar cfe Launcher.jar com.craftlauncher.Main -C build .
if exist "dist\MinecraftLauncher\app" (
    copy /Y Launcher.jar "dist\MinecraftLauncher\app\Launcher.jar" >nul
)

echo [3/3] Compiling native MinecraftLauncher.exe...
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /target:winexe /optimize+ /platform:anycpu /win32icon:app.ico /out:MinecraftLauncher.exe LauncherBootstrap.cs >nul

echo.
echo ===================================================
echo  Build finished successfully!
echo  Run: MinecraftLauncher.exe or Launcher.jar
echo ===================================================
endlocal
