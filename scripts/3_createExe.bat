
@echo OFF
@chcp 65001


rem 変数設定 -------------------------------------------------------------------

set VERSION=0.7.4

set JLINK_CMD=c:\pleiades_202006\java\14\bin\jlink
set EXEWRAP_CMD=exewrap1.6.2\x64\exewrap.exe

set JRE_WIN=C:\UserLibs\jdk-14.0.2+12-win-x64\jmods
set JFX_WIN=C:\UserLibs\javafx-jmods-14.0.2.1-win-x64

set SOURCE_JAR=..\xyz.hotchpotch.hogandiff\build\libs\xyz.hotchpotch.hogandiff.jar

set OUTPUT_COMMON=..\build\方眼Diff-%VERSION%
set OUTPUT_WIN=%OUTPUT_COMMON%-win64

set REQUIRED_MOD=^
java.base,^
java.desktop,^
java.xml,^
javafx.base,^
javafx.controls,^
javafx.fxml,^
javafx.graphics

set ADDITIONAL_MOD=^
jdk.charsets,^
jdk.zipfs


rem Windows x64向け配布物の生成 ------------------------------------------------

echo.
echo -- 出力フォルダの削除

rmdir /s /q %OUTPUT_WIN%

echo.
echo -- 出力フォルダの作成とリソースのコピー

xcopy %OUTPUT_COMMON% %OUTPUT_WIN% /e /i
copy ..\resources\方眼Diff.exe.vmoptions %OUTPUT_WIN%

echo.
echo -- 最小構成JREの作成

%JLINK_CMD% ^
--compress=2 ^
--module-path %JRE_WIN%;%JFX_WIN% ^
--add-modules %REQUIRED_MOD%,%ADDITIONAL_MOD% ^
--output %OUTPUT_WIN%\jre-min

echo.
echo -- EXEファイルの作成

%EXEWRAP_CMD% ^
-g ^
-t 14 ^
-i ..\resources\favicon.ico ^
-v %VERSION% ^
-V %VERSION% ^
-d "方眼Diff" ^
-c "(c) 2020 nmby" ^
-p "方眼Diff" ^
-j %SOURCE_JAR% ^
-o %OUTPUT_WIN%\方眼Diff.exe
