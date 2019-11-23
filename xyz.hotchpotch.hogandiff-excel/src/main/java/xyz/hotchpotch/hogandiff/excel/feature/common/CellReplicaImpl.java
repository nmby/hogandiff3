package xyz.hotchpotch.hogandiff.excel.feature.common;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link CellReplica} の標準的な実装です。<br>
 * {@code T} 型のオブジェクトが不変であれば、このクラスのオブジェクトも不変です。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
@Deprecated
public class CellReplicaImpl<T> extends CellReplica<T> {
    
    // [static members] ********************************************************
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param <T> セルデータの型
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @param data セルデータ
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code data} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static <T> CellReplica<T> of(int row, int column, T data) {
        Objects.requireNonNull(data, "data");
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return new CellReplicaImpl<>(row, column, data);
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param <T> セルデータの型
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @return 新たな空のセルレプリカ
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static <T> CellReplica<T> empty(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return new CellReplicaImpl<>(row, column, null);
    }
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param <T> セルデータの型
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param data セルデータ
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code address}, {@code data} のいずれかが {@code null} の場合
     */
    public static <T> CellReplica<T> of(String address, T data) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(data, "data");
        
        Pair<Integer> idx = CellId.addressToIdx(address);
        return new CellReplicaImpl<>(idx.a(), idx.b(), data);
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param <T> セルデータの型
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 新たな空のセルレプリカ
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static <T> CellReplica<T> empty(String address) {
        Objects.requireNonNull(address, "address");
        
        Pair<Integer> idx = CellId.addressToIdx(address);
        return new CellReplicaImpl<>(idx.a(), idx.b(), null);
    }
    
    // [instance members] ******************************************************
    
    private final int row;
    private final int column;
    private final T data;
    
    private CellReplicaImpl(int row, int column, T data) {
        super(CellId.of(row, column), data);
        
        assert 0 <= row;
        assert 0 <= column;
        
        this.row = row;
        this.column = column;
        this.data = data;
    }
    
    public int row() {
        return row;
    }
    
    public int column() {
        return column;
    }
    
    @Override
    public T data() {
        return data;
    }
    
    /**
     * {@code o} も {@link CellReplicaImpl} であり、
     * {@link CellReplicaImpl#row()}, {@link CellReplicaImpl#column()} の値がそれぞれ等しく、
     * {@link CellReplicaImpl#data()} が同値と判定される場合に
     * {@code true} を返します。<br>
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof CellReplicaImpl) {
            CellReplicaImpl<?> other = (CellReplicaImpl<?>) o;
            return row == other.row()
                    && column == other.column()
                    && Objects.equals(data, other.data());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(row, column, data);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", CellId.idxToAddress(row, column), data == null ? "" : data);
    }
}
