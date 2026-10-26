#!/bin/sh
# Compila o projeto e gera o Xadrez.jar (requer JDK instalado).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name '*.java')
printf 'Main-Class: xadrez.Xadrez\n' > out/manifest.txt
jar cfm Xadrez.jar out/manifest.txt -C out xadrez
echo "Pronto! Execute com: java -jar Xadrez.jar"
