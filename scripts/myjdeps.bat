
@echo OFF
chcp 65001

set JDEPS_CMD=c:\pleiades_201909\java\13\bin\jdeps
set VERSION=0.6.3
set ENV=win64

set MODULE_PATH_JRE=C:\pleiades_201909\java\13\jmods
set MODULE_PATH_JAVAFX=C:\Users\ya_na\OneDrive\UserLibs\javafx-sdk-13\lib
set MODULE_PATH_POI=C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\poi-4.1.1.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\poi-ooxml-4.1.1.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\poi-ooxml-schemas-4.1.1.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\lib\commons-codec-1.13.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\lib\commons-collections4-4.4.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\lib\commons-compress-1.19.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\lib\commons-math3-3.6.1.jar;C:\Users\ya_na\OneDrive\UserLibs\poi-4.1.1\ooxml-lib\xmlbeans-3.1.0.jar
set DIR_JARS=..\build\方眼Diff-%VERSION%-%ENV%\lib\hogandiff-%VERSION%
set MODULE_PATH_JARS=%DIR_JARS%\xyz.hotchpotch.hogandiff.util.jar;%DIR_JARS%\xyz.hotchpotch.hogandiff.core.jar;%DIR_JARS%\xyz.hotchpotch.hogandiff.excel.jar

set TARGET_JAR=..\build\jar\xyz.hotchpotch.hogandiff.gui.jar

%JDEPS_CMD% --module-path %MODULE_PATH_JRE%;%MODULE_PATH_JAVAFX%;%MODULE_PATH_POI%;%MODULE_PATH_JARS% -s %TARGET_JAR%
