
@echo OFF
@chcp 65001


echo.
echo 各種パラメータ -----------------------------------------------------------------

set VERSION=0.7.4

set PJ_HOME=..
set PJ_RESOURCES=%PJ_HOME%\resources

set OUTPUT_COMMON=%PJ_HOME%\build\方眼Diff-%VERSION%
set OUTPUT_WIN64=%PJ_HOME%\build\方眼Diff-%VERSION%-win64
set OUTPUT_MACOS=%PJ_HOME%\build\方眼Diff-%VERSION%-macos

set USER_LIB=C:\UserLibs

set JRE_WIN64=%USER_LIB%\jdk-14.0.2+12-win-x64\jmods
set JRE_MACOS=%USER_LIB%\jdk-14.0.2+12-mac-x64\Contents\Home\jmods
set JFX_WIN64=%USER_LIB%\javafx-jmods-14.0.2.1-win-x64
set JFX_MACOS=%USER_LIB%\javafx-jmods-14.0.2.1-mac-x64

set SOURCE_JAR=%PJ_HOME%\xyz.hotchpotch.hogandiff\build\libs\xyz.hotchpotch.hogandiff.jar

set JLINK_CMD=c:\pleiades_202006\java\14\bin\jlink
set EXEWRAP_CMD=exewrap1.6.2\x64\exewrap.exe

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


echo.
echo 出力フォルダの削除 -------------------------------------------------------------

rmdir /s /q %OUTPUT_COMMON%\
rmdir /s /q %OUTPUT_WIN64%\
rmdir /s /q %OUTPUT_MACOS%\

echo.
echo 必要ライブラリの収集 -----------------------------------------------------------

xcopy %USER_LIB%\poi-4.1.2\LICENSE                          %OUTPUT_COMMON%\lib\poi-4.1.2\
xcopy %USER_LIB%\poi-4.1.2\NOTICE                           %OUTPUT_COMMON%\lib\poi-4.1.2\
xcopy %USER_LIB%\poi-4.1.2\poi-4.1.2.jar                    %OUTPUT_COMMON%\lib\poi-4.1.2\
xcopy %USER_LIB%\poi-4.1.2\poi-ooxml-4.1.2.jar              %OUTPUT_COMMON%\lib\poi-4.1.2\
xcopy %USER_LIB%\poi-4.1.2\poi-ooxml-schemas-4.1.2.jar      %OUTPUT_COMMON%\lib\poi-4.1.2\
xcopy %USER_LIB%\poi-4.1.2\lib\commons-codec-1.13.jar       %OUTPUT_COMMON%\lib\poi-4.1.2\lib\
xcopy %USER_LIB%\poi-4.1.2\lib\commons-collections4-4.4.jar %OUTPUT_COMMON%\lib\poi-4.1.2\lib\
xcopy %USER_LIB%\poi-4.1.2\lib\commons-compress-1.19.jar    %OUTPUT_COMMON%\lib\poi-4.1.2\lib\
xcopy %USER_LIB%\poi-4.1.2\lib\commons-math3-3.6.1.jar      %OUTPUT_COMMON%\lib\poi-4.1.2\lib\
xcopy %USER_LIB%\poi-4.1.2\lib\SparseBitSet-1.2.jar         %OUTPUT_COMMON%\lib\poi-4.1.2\lib\
xcopy %USER_LIB%\poi-4.1.2\ooxml-lib\xmlbeans-3.1.0.jar     %OUTPUT_COMMON%\lib\poi-4.1.2\ooxml-lib\

echo.
echo 最小構成JREの作成 (Windows x64向け) --------------------------------------------

%JLINK_CMD% ^
--compress=2 ^
--module-path %JRE_WIN64%;%JFX_WIN64% ^
--add-modules %REQUIRED_MOD%,%ADDITIONAL_MOD% ^
--output %OUTPUT_COMMON%\jre-min-win64\

echo.
echo 最小構成JREの作成 (MacOS向け) --------------------------------------------------

%JLINK_CMD% ^
--compress=2 ^
--module-path %JRE_MACOS%;%JFX_MACOS% ^
--add-modules %REQUIRED_MOD%,%ADDITIONAL_MOD% ^
--output %OUTPUT_COMMON%\jre-min-macos\

echo.
echo 配布パッケージの作成 (Windows x64向け) -----------------------------------------

mkdir %OUTPUT_WIN64%

copy %PJ_HOME%\README.md                    %OUTPUT_WIN64%\
copy %PJ_HOME%\LICENSE                      %OUTPUT_WIN64%\
copy %PJ_RESOURCES%\方眼Diff.exe.vmoptions  %OUTPUT_WIN64%\
xcopy /e %OUTPUT_COMMON%\lib                %OUTPUT_WIN64%\lib\
xcopy /e %OUTPUT_COMMON%\jre-min-win64      %OUTPUT_WIN64%\jre-min\

%EXEWRAP_CMD% ^
-g ^
-t 14 ^
-i %PJ_RESOURCES%\favicon.ico ^
-v %VERSION% ^
-V %VERSION% ^
-d "方眼Diff" ^
-c "(c) 2020 nmby" ^
-p "方眼Diff" ^
-j %SOURCE_JAR% ^
-o %OUTPUT_WIN64%\方眼Diff.exe

echo.
echo 配布パッケージの作成 (MacOS向け) -----------------------------------------------

mkdir %OUTPUT_MACOS%

xcopy /e %PJ_RESOURCES%\方眼Diff.app        %OUTPUT_MACOS%\方眼Diff.app\
xcopy /e %OUTPUT_COMMON%\lib                %OUTPUT_MACOS%\方眼Diff.app\Contents\PlugIns\lib\
xcopy /e %OUTPUT_COMMON%\jre-min-macos      %OUTPUT_MACOS%\方眼Diff.app\Contents\PlugIns\jre-min\

copy %SOURCE_JAR%           %OUTPUT_MACOS%\方眼Diff.app\Contents\Java\
copy %PJ_HOME%\README.md    %OUTPUT_MACOS%\
copy %PJ_HOME%\LICENSE      %OUTPUT_MACOS%\
