
@echo OFF
chcp 65001


rem COMMON ---------------------------------------------------------------------

set VERSION=0.7.2

set JLINK_CMD=c:\pleiades_202006\java\14\bin\jlink

set REQUIRED_MOD=java.base,java.desktop,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics
set ADDITIONAL_MOD=jdk.charsets,jdk.zipfs

set OUTPUT_COMMON=..\build\方眼Diff-%VERSION%


rem WIN ------------------------------------------------------------------------

set OUTPUT_WIN=%OUTPUT_COMMON%-win64

rmdir/s /q %OUTPUT_WIN%
xcopy %OUTPUT_COMMON% %OUTPUT_WIN% /e /i
copy ..\resources\方眼Diff.exe.vmoptions %OUTPUT_WIN%

set JRE_WIN=C:\pleiades_202006\java\14\jmods
set JFX_WIN=C:\Users\ya_na\OneDrive\UserLibs\javafx-jmods-13

%JLINK_CMD% --compress=2 --module-path %JRE_WIN%;%JFX_WIN% --add-modules %REQUIRED_MOD%,%ADDITIONAL_MOD% --output %OUTPUT_WIN%\jre-min

set EXEWRAP_CMD=exewrap1.5.0\x64\exewrap.exe

%EXEWRAP_CMD% -g -t 14 -i ..\resources\favicon.ico -v %VERSION% -V %VERSION% -d "方眼Diff" -c "(c) 2020 nmby" -p "方眼Diff" -j ..\xyz.hotchpotch.hogandiff\build\libs\xyz.hotchpotch.hogandiff.jar -o %OUTPUT_WIN%\方眼Diff.exe
