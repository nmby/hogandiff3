package xyz.hotchpotch.hogandiff.excel;

import java.util.HashMap;
import java.util.Map;
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
public class CellReplica<T> {
    
    // [static members] ********************************************************
    
    /**
     * セルの識別子を表す不変クラスです。<br>
     * 
     * @author nmby
     */
    public static class CellId implements Comparable<CellId> {
        
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
        
        @Override
        public int compareTo(CellId o) {
            return row != o.row
                    ? Integer.compare(row, o.row)
                    : Integer.compare(column, o.column);
        }
    }
    
    /**
     * セル内容物の種類（例えば、セル内文字列、セルコメント、罫線など）を表します。<br>
     * 
     * @author nmby
     */
    public static interface CellContentType<U> {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        String tag();
    }
    
    public static <U> CellReplica<?> of(int row, int column, CellContentType<U> type, U content) {
        Objects.requireNonNull(type, "type");
        
        return new CellReplica<>(CellId.of(row, column), type, content);
    }
    
    /**
     * 空の {@link CellReplica} を生成して返します。<br>
     * 
     * @param <T> セルデータの型
     * @param row 行番号（0開始）
     * @param column 列番号（0開始）
     * @return 空のセル
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが範囲外の場合
     */
    public static <T> CellReplica<T> empty(int row, int column) {
        return new CellReplica<>(CellId.of(row, column));
    }
    
    // [instance members] ******************************************************
    
    private final CellId id;
    
    private final Map<CellContentType<?>, Object> contents = new HashMap<>();
    
    @Deprecated
    private final T data;
    
    private <U> CellReplica(CellId id, CellContentType<U> type, U content) {
        assert id != null;
        assert type != null;
        
        this.id = id;
        this.data = null;
        contents.put(type, content);
    }
    
    private CellReplica(CellId id) {
        assert id != null;
        
        this.id = id;
        this.data = null;
    }
    
    /**
     * セルの識別子を返します。<br>
     * 
     * @return セルの識別子
     */
    public CellId id() {
        return id;
    }
    
    /**
     * セルデータを返します。<br>
     * 
     * @return セルデータ
     */
    @Deprecated
    public T data() {
        return data;
    }
    
    /**
     * 指定された種類のセル内容物を返します。<br>
     * 
     * @param <U> セル内容物の型
     * @param type セル内容物の種類
     * @return セル内容物（格納されていない場合は {@code null}）
     * @throws NullPointerException {@code type} が {@code null} の場合
     */
    @SuppressWarnings("unchecked")
    public <U> U getContent(CellContentType<U> type) {
        Objects.requireNonNull(type, "type");
        
        return (U) contents.get(type);
    }
    
    /**
     * このセルに内容物を追加します。<br>
     * 
     * @param <U> セル内容物の型
     * @param type セル内容物の種類
     * @param content セル内容物
     * @throws NullPointerException {@code type} が {@code null} の場合
     */
    public <U> void setContent(CellContentType<U> type, U content) {
        Objects.requireNonNull(type, "type");
        
        contents.put(type, content);
    }
    
    /**
     * 指定されたセルの全ての内容物をこのセルに追加します。<br>
     * 
     * @param other このセルに追加する内容物を保持するセル
     * @throws NullPointerException {@code other} が {@code null} の場合
     * @throws IllegalArgumentException このセルと {@code other} の id が異なる場合
     * @throws IllegalArgumentException {@code other} が保持する内容物をこのセルが既に保持している場合
     */
    public void addAll(CellReplica<?> other) {
        Objects.requireNonNull(other, "other");
        if (!id.equals(other.id)) {
            throw new IllegalArgumentException(String.format("idが異なります: %s vs %s", id, other.id));
        }
        
        other.contents.forEach((type, content) -> {
            if (contents.containsKey(type)) {
                throw new IllegalArgumentException("内容物が既に格納されています: " + type.tag());
            }
            contents.put(type, content);
        });
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof CellReplica) {
            CellReplica<?> other = (CellReplica<?>) o;
            return id.equals(other.id) && data.equals(other.data) && contents.equals(other.contents);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, data, contents);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", id, data);
    }
}
