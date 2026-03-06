@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
  echo 未检测到系统 gradle，请先安装（例如通过 choco 或 scoop）
  exit /b 1
)
gradle %*
