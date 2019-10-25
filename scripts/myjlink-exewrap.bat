
@echo OFF
chcp 65001

set VERSION=0.6.1
set ENV=win64

set JLINK_CMD=c:\pleiades_201909\java\13\bin\jlink

set MODULE_PATH_JRE=C:\pleiades_201909\java\13\jmods
set MODULE_PATH_JAVAFX=C:\Users\ya_na\OneDrive\UserLibs\javafx-jmods-13

set TARGET_MODULES=java.base,java.desktop,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics
set ADDITIONAL_TARGET_MODULES=jdk.charsets,jdk.zipfs

set OUTPUT_DIR=..\build\方眼Diff-%VERSION%-%ENV%

%JLINK_CMD% --compress=2 --module-path %MODULE_PATH_JRE%;%MODULE_PATH_JAVAFX% --add-modules %TARGET_MODULES%,%ADDITIONAL_TARGET_MODULES% --output %OUTPUT_DIR%\jre-min

set EXEWRAP_CMD=exewrap1.4.2\x64\exewrap.exe

%EXEWRAP_CMD% -g -t 11 -i ..\resources\favicon.ico -v %VERSION% -V %VERSION% -d "方眼Diff" -c "(c) 2019 nmby" -p "方眼Diff" -j ..\build\jar\xyz.hotchpotch.hogandiff.gui.jar -o %OUTPUT_DIR%\方眼Diff.exe
