package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import org.apache.poi.ss.util.CellAddress;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelシート上のセルを表します。<br>
 *
 * @author nmby
 */
public class CellReplica {
    
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
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @param content セル内容
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code content} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellReplica of(int row, int column, String content) {
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return new CellReplica(row, column, content);
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @return 新たな空のセルレプリカ
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellReplica empty(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return new CellReplica(row, column, null);
    }
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param content セル内容
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code address}, {@code content} のいずれかが {@code null} の場合
     */
    public static CellReplica of(String address, String content) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(content, "content");
        
        Pair<Integer> idx = CellReplica.addressToIdx(address);
        return new CellReplica(idx.a(), idx.b(), content);
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 新たな空のセルレプリカ
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static CellReplica empty(String address) {
        Objects.requireNonNull(address, "address");
        
        Pair<Integer> idx = CellReplica.addressToIdx(address);
        return new CellReplica(idx.a(), idx.b(), null);
    }
    
    // [instance members] ******************************************************
    
    private final int row;
    private final int column;
    private final String content;
    
    private CellReplica(int row, int column, String content) {
        assert 0 <= row;
        assert 0 <= column;
        
        this.row = row;
        this.column = column;
        this.content = content;
    }
    
    /**
     * 行インデックス（0開始）を返します。<br>
     * 
     * @return 行インデックス（0開始）
     */
    public int row() {
        return row;
    }
    
    /**
     * 列インデックス（0開始）を返します。<br>
     * 
     * @return 列インデックス（0開始）
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
        return idxToAddress(row(), column());
    }
    
    /**
     * セル内容を返します。<br>
     * 
     * @return セル内容
     */
    public String content() {
        return content;
    }
    
    /**
     * {@code o} も {@link CellReplica} であり、
     * {@link CellReplica#row()}, {@link CellReplica#column()} の値がそれぞれ等しく、
     * {@link CellReplica#content()} が同値と判定される場合に
     * {@code true} を返します。<br>
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof CellReplica) {
            CellReplica other = (CellReplica) o;
            return row == other.row()
                    && column == other.column()
                    && Objects.equals(content, other.content());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(row, column, content);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", address(), content == null ? "" : content);
    }
}
