
@echo OFF
chcp 65001

set JDEPS_CMD=c:\pleiades_202006\java\14\bin\jdeps

set MODULE_PATH_JRE=C:\UserLibs\jdk-14.0.2+12-win-x64\jmods
set MODULE_PATH_JAVAFX=C:\UserLibs\javafx-sdk-14.0.2.1-win-x64\lib
set PATH_POI=C:\UserLibs\poi-4.1.2
set MODULE_PATH_POI=^
%PATH_POI%\poi-4.1.2.jar;^
%PATH_POI%\poi-ooxml-4.1.2.jar;^
%PATH_POI%\poi-ooxml-schemas-4.1.2.jar;^
%PATH_POI%\lib\commons-codec-1.13.jar;^
%PATH_POI%\lib\commons-collections4-4.4.jar;^
%PATH_POI%\lib\commons-compress-1.19.jar;^
%PATH_POI%\lib\commons-math3-3.6.1.jar;^
%PATH_POI%\lib\SparseBitSet-1.2.jar;^
%PATH_POI%\ooxml-lib\xmlbeans-3.1.0.jar

set TARGET_JAR=..\xyz.hotchpotch.hogandiff\build\libs\xyz.hotchpotch.hogandiff.jar

%JDEPS_CMD% --module-path %MODULE_PATH_JRE%;%MODULE_PATH_JAVAFX%;%MODULE_PATH_POI% -s %TARGET_JAR%
