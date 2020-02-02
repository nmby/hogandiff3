package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import org.apache.poi.ss.util.CellAddress;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelシート上のセルを表します。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
// 設計メモ：
// 本アプリでは、シート上の要素として「セル」、つまり行・列で特定される要素を
// 基本的な単位とする。そして、セルのデータ型に柔軟性を持たせる。
// 将来的には、図形オブジェクト等も扱えるようにしたい。
// この場合は行・列以外の識別子が必要だからもう一段の抽象化が必要となるが、
// それは将来のバージョンに譲ることとする。
public interface CellReplica<T> {
    
    // [static members] ********************************************************
    
    /**
     * 行・列のインデックス（{@code (0, 0)} 形式）を
     * セルアドレス（{@code "A1"} 形式）に変換します。<br>
     * 
     * @param row 行インデックス（0 開始）
     * @param column 列インデックス（0 開始）
     * @return セルアドレス（{@code "A1"} 形式）
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static String idxToAddress(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("row:%d, column:%d", row, column));
        }
        return new CellAddress(row, column).formatAsString();
    }
    
    /**
     * セルアドレス（{@code "A1"} 形式）を
     * 行・列のインデックス（{@code (0, 0)} 形式）に変換します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 行・列のインデックスのペア
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static Pair<Integer> addressToIdx(String address) {
        Objects.requireNonNull(address, "address");
        
        CellAddress ca = new CellAddress(address);
        return Pair.of(ca.getRow(), ca.getColumn());
    }
    
    /**
     * 列のインデックス（{@code 0} など）を記号（{@code "A"} など）に変換します。<br>
     * 
     * @param column 列インデックス（0 開始）
     * @return 列の記号（{@code "A"} など）
     * @throws IndexOutOfBoundsException {@code column} が 0 未満の場合
     */
    public static String columnIdxToStr(int column) {
        if (column < 0) {
            throw new IndexOutOfBoundsException(String.format("column:%d", column));
        }
        
        String address = new CellAddress(0, column).formatAsString();
        return address.substring(0, address.length() - 1);
    }
    
    /**
     * 列の記号（{@code "A"} など）を列のインデックス（{@code 0} など）に変換します。<br>
     * 
     * @param columnStr 列の記号（{@code "A"} など）
     * @return 列インデックス（0 開始）
     * @throws NullPointerException {@code columnStr} が {@code null} の場合
     */
    public static int columnStrToIdx(String columnStr) {
        Objects.requireNonNull(columnStr, "columnStr");
        
        CellAddress ca = new CellAddress(columnStr + "1");
        return ca.getColumn();
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行インデックス（0開始）を返します。<br>
     * 
     * @return 行インデックス（0開始）
     */
    int row();
    
    /**
     * 列インデックス（0開始）を返します。<br>
     * 
     * @return 列インデックス（0開始）
     */
    int column();
    
    /**
     * セルアドレス（{@code "A1"} 形式）を返します。<br>
     * 
     * @return セルアドレス（{@code "A1"} 形式）
     */
    default String address() {
        return idxToAddress(row(), column());
    }
    
    /**
     * セルデータを返します。<br>
     * 
     * @return セルデータ
     */
    T data();
}
