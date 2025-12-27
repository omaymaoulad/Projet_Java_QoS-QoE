@echo off
echo ************************************************
echo *   CREATION APPLICATION EXE - QoS/QoE        *
echo ************************************************
echo.

REM Étape 1: Vérifications
echo [1/5] Verification des outils...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Java n'est pas installe!
    pause
    exit /b 1
)

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Maven n'est pas installe!
    pause
    exit /b 1
)

echo Java et Maven sont OK.
echo.

REM Étape 2: Nettoyage
echo [2/5] Nettoyage du projet...
call mvn clean

if %errorlevel% neq 0 (
    echo ERREUR lors du nettoyage
    pause
    exit /b 1
)

echo Nettoyage termine.
echo.

REM Étape 3: Compilation
echo [3/5] Compilation du projet...
call mvn compile

if %errorlevel% neq 0 (
    echo ERREUR de compilation
    echo Verifiez votre code Java.
    pause
    exit /b 1
)

echo Compilation reussie.
echo.

REM Étape 4: Création du JAR
echo [4/5] Creation du fichier JAR...
call mvn package -DskipTests

if %errorlevel% neq 0 (
    echo ERREUR lors de la creation du JAR
    pause
    exit /b 1
)

echo JAR cree avec succes.
echo.

REM Étape 5: Création de l'EXE
echo [5/5] Creation de l'executable .exe...
echo Cette etape peut prendre plusieurs minutes...
echo.

REM Methode 1: Avec jpackage
if exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo Utilisation de jpackage...
    "%JAVA_HOME%\bin\jpackage" ^
      --name "QoS_QoE_Analyzer" ^
      --input target ^
      --main-jar "QoS-QoE-System-1.0-full.jar" ^
      --main-class com.ensah.qoe.Main ^
      --type exe ^
      --dest "application-exe" ^
      --vendor "ENSAH" ^
      --app-version "1.0.0" ^
      --win-shortcut ^
      --win-menu ^
      --win-menu-group "ENSAH" ^
      --java-options "-Xmx1024m"

    if %errorlevel% equ 0 goto SUCCES
)

REM Methode 2: Avec Launch4j (alternative)
echo jpackage non disponible, tentative alternative...
echo Telechargez Launch4j depuis:
echo http://launch4j.sourceforge.net/
echo.
echo Ou utilisez cette commande manuelle:
echo.
echo Pour creer l'EXE manuellement:
echo 1. Allez dans le dossier: target
echo 2. Executez: java -jar "QoS-QoE-System-1.0-full.jar"
echo 3. Si ca marche, vous pouvez utiliser Launch4j pour creer l'EXE
goto FIN

:SUCCES
echo.
echo ============================================
echo          OPERATION REUSSIE !
echo ============================================
echo Votre application .exe a ete creee avec succes!
echo.
echo Emplacement: %cd%\application-exe\
echo Fichier: QoS_QoE_Analyzer.exe
echo.
echo Pour l'installer: double-cliquez sur l'executable
echo ============================================
echo.

:FIN
pause