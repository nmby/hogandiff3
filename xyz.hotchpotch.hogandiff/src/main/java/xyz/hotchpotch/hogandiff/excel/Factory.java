package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;

import xyz.hotchpotch.hogandiff.excel.feature.basic.BasicFactory;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理に必要な一連の機能を提供するファクトリです。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
public interface Factory<T> {
    
    // [static members] ********************************************************
    
    /**
     * セルの内容を文字列として比較するためのファクトリを返します。<br>
     * 
     * @return セルの内容を文字列として比較するためのファクトリ
     */
    public static Factory<String> basicFactoryOf() {
        return BasicFactory.of();
    }
    
    // FIXME: [No.99 機能追加] 将来的には basic 以外の feature も提供したい。
    // セル書式を比較する feature とか、セルコメントを比較する feature とか、
    // オブジェクト内のテキストを比較する feature とか、
    // それらを統合的に扱う feature とか。
    
    // [instance members] ******************************************************
    
    /**
     * Excelブックからシート名の一覧を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    BookLoader bookLoader(Path bookPath)
            throws ExcelHandlingException;
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excelブックのパス
     * @return Excelシートからセルデータを抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    SheetLoader<T> sheetLoader(Settings settings, Path bookPath)
            throws ExcelHandlingException;
    
    /**
     * 2つのExcelシートから抽出したセルセット同士を比較するコンパレータを返します。<br>
     * 
     * @param settings 設定
     * @return セルセット同士を比較するコンパレータ
     */
    SComparator<T> comparator(Settings settings);
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excelブックのパス
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    BookPainter painter(Settings settings, Path bookPath)
            throws ExcelHandlingException;
}
