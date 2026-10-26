@echo off
rem Compila o projeto e gera o Xadrez.jar (requer JDK instalado).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
if errorlevel 1 goto erro
echo Main-Class: xadrez.Xadrez> out\manifest.txt
jar cfm Xadrez.jar out\manifest.txt -C out xadrez
del fontes.txt
echo Pronto! Execute com: java -jar Xadrez.jar
goto fim
:erro
echo Falha na compilacao.
:fim
pause
