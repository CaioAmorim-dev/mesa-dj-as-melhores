@echo off
chcp 65001 >nul
if not exist out mkdir out
javac -encoding UTF-8 -d out src/br/com/cesar/dj/*.java
if %errorlevel% neq 0 (
    echo [ERRO] Falha na compilacao do projeto.
    pause
    exit /b %errorlevel%
)
java -cp out br.com.cesar.dj.Main
