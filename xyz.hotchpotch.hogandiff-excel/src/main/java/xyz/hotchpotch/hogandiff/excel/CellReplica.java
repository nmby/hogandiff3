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
public abstract class CellReplica<T> {
    
    // [static members] ********************************************************
    
    /**
     * セルの識別子を表す不変クラスです。<br>
     * 
     * @author nmby
     */
    public static class CellId {
        
        // [static members] ----------------------------------------------------
        
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
        
        public static CellId of(int row, int column) {
            if (row < 0 || column < 0) {
                throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
            }
            return new CellId(row, column);
        }
        
        // [instance members] --------------------------------------------------
        
        private final int row;
        private final int column;
        
        private CellId(int row, int column) {
            assert 0 <= row;
            assert 0 <= column;
            
            this.row = row;
            this.column = column;
        }
        
        /**
         * 行番号（0開始）を返します。<br>
         * 
         * @return 行番号（0開始）
         */
        public int row() {
            return row;
        }
        
        /**
         * 列番号（0開始）を返します。<br>
         * 
         * @return 列番号（0開始）
         */
        public int column() {
            return column;
        }
        
        /**
         * セルアドレス（{@code "A1"} 形式）を返します。<br>
         * 
         * @return セルアドレス（{@code "A1"} 形式）
         */
        public String address() {
            return idxToAddress(row, column);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof CellId) {
                CellId other = (CellId) o;
                return row == other.row && column == other.column;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(row, column);
        }
        
        @Override
        public String toString() {
            return address();
        }
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行インデックス（0開始）を返します。<br>
     * 
     * @return 行インデックス（0開始）
     */
    @Deprecated
    public abstract int row();
    
    /**
     * 列インデックス（0開始）を返します。<br>
     * 
     * @return 列インデックス（0開始）
     */
    @Deprecated
    public abstract int column();
    
    /**
     * セルアドレス（{@code "A1"} 形式）を返します。<br>
     * 
     * @return セルアドレス（{@code "A1"} 形式）
     */
    @Deprecated
    public String address() {
        return CellId.idxToAddress(row(), column());
    }
    
    /**
     * セルの識別子を返します。<br>
     * 
     * @return セルの識別子
     */
    public CellId id() {
        throw new UnsupportedOperationException("これから実装します。");
    }
    
    /**
     * セルデータを返します。<br>
     * 
     * @return セルデータ
     */
    public abstract T data();
}
