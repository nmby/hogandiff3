package xyz.hotchpotch.hogandiff.excel;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

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
     * 指定された2つのセルの {@code #row()}, {@code #column()} を除く属性値が等しいかを返します。<br>
     * 
     * @param cell1 比較対象のセル1（{@code null} 許容）
     * @param cell2 比較対象のセル2（{@code null} 許容）
     * @return {@code cell1}, {@code cell2} が {@code null} でなく属性値が等しい場合は {@code true}
     */
    public static boolean attrEquals(CellReplica cell1, CellReplica cell2) {
        return cell1 == cell2 || (cell1 != null && cell1.attrEquals(cell2));
    }
    
    /**
     * 指定された2つのセルの {@code #row()}, {@code #column()} を除く属性値の大小関係を返します。<br>
     * 
     * @param cell1 比較対象のセル1
     * @param cell2 比較対象のセル2
     * @return セル1の属性値がセル2の属性値より小さい場合は負の整数、等しい場合は0、大きい場合は正の整数
     * @throws NullPointerException {@code cell1}, {@code cell2} のいずれかが {@code null} の場合
     */
    public static int attrCompare(CellReplica cell1, CellReplica cell2) {
        Objects.requireNonNull(cell1, "cell1");
        Objects.requireNonNull(cell2, "cell2");
        
        return cell1.attrCompareTo(cell2);
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
    private String content;
    private String comment;
    
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
    public String getContent() {
        return content;
    }
    
    /**
     * セル内容を設定します。<br>
     * 
     * @param content セル内容
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * セルコメントを返します。<br>
     * 
     * @return セルコメント
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * セルコメントを設定します。<br>
     * 
     * @param comment セルコメント
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * {@code #row()}, {@code #column()} を除く属性について、
     * このセルと指定されたセルの属性値が等しいかを返します。<br>
     * 
     * @param cell 比較対象のセル（{@code null} 許容）
     * @return {@code cell} が {@code null} でなく属性値が等しい場合は {@code true}
     */
    public boolean attrEquals(CellReplica cell) {
        return cell != null
                && Objects.equals(content, cell.content)
                && Objects.equals(comment, cell.comment);
    }
    
    /**
     * {@code #row()}, {@code #column()} を除く属性について、
     * このセルと指定されたセルの属性値の大小関係を返します。<br>
     * 
     * @param cell 比較対象のセル
     * @return このセルの属性値が指定されたセルの属性値より
     *          小さい場合は負の整数、等しい場合はゼロ、大きい場合は正の整数
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    public int attrCompareTo(CellReplica cell) {
        Objects.requireNonNull(cell, "cell");
        
        int compare1 = Objects.compare(
                Optional.ofNullable(content).orElse(""),
                Optional.ofNullable(cell.content).orElse(""),
                Comparator.naturalOrder());
        if (compare1 != 0) {
            return compare1;
        }
        
        return Objects.compare(
                Optional.ofNullable(comment).orElse(""),
                Optional.ofNullable(cell.comment).orElse(""),
                Comparator.naturalOrder());
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
                    && attrEquals(other);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(row, column, content);
    }
    
    @Override
    public String toString() {
        return String.format(
                "%s: %s%s%s",
                address(),
                content == null ? "" : content,
                (content != null && comment != null) ? " " : "",
                (comment == null || "".equals(comment)) ? "" : "[comment: " + comment + "]");
    }
}
