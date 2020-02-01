package xyz.hotchpotch.hogandiff.excel;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelシート同士の比較結果を表す不変クラスです。<br>
 * 
 * @param <T> セルデータの型
 * @author nmby
 */
public class SResult<T> {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * 片側のシートに関する差分内容を表す不変クラスです。<br>
     *
     * @param <T> セルデータの型
     * @author nmby
     */
    public static class Piece<T> {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final List<Integer> redundantRows;
        private final List<Integer> redundantColumns;
        private final List<CellReplica<T>> diffCells;
        
        private Piece(
                List<Integer> redundantRows,
                List<Integer> redundantColumns,
                List<CellReplica<T>> diffCells) {
            
            assert redundantRows != null;
            assert redundantColumns != null;
            assert diffCells != null;
            
            // 一応防御的コピーしておく。
            this.redundantRows = List.copyOf(redundantRows);
            this.redundantColumns = List.copyOf(redundantColumns);
            this.diffCells = List.copyOf(diffCells);
        }
        
        /**
         * 余剰行のインデックスを返します。<br>
         * 
         * @return 余剰行のインデックス
         */
        public List<Integer> redundantRows() {
            return redundantRows;
        }
        
        /**
         * 余剰列のインデックスを返します。<br>
         * 
         * @return 余剰列のインデックス
         */
        public List<Integer> redundantColumns() {
            return redundantColumns;
        }
        
        /**
         * 差分セルを返します。<br>
         * 
         * @return 差分セル
         */
        public List<CellReplica<T>> diffCells() {
            return diffCells;
        }
    }
    
    /**
     * Excelシート同士の比較結果を生成します。<br>
     * 
     * @param <T> セルデータの型
     * @param considerRowGaps 比較において行の余剰／欠損を考慮したか
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮したか
     * @param redundantRows1 シート1における余剰行
     * @param redundantRows2 シート2における余剰行
     * @param redundantColumns1 シート1における余剰列
     * @param redundantColumns2 シート2における余剰列
     * @param diffCells 差分セル
     * @return Excelシート同士の比較結果
     * @throws NullPointerException
     *              {@code redundantRows1}, {@code redundantRows2},
     *              {@code redundantColumns1}, {@code redundantColumns2},
     *              {@code diffCells} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              余剰／欠損の考慮なしにも関わらす余剰／欠損の数が 0 でない場合
     */
    public static <T> SResult<T> of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            List<Integer> redundantRows1,
            List<Integer> redundantRows2,
            List<Integer> redundantColumns1,
            List<Integer> redundantColumns2,
            List<Pair<CellReplica<T>>> diffCells) {
        
        Objects.requireNonNull(redundantRows1, "redundantRows1");
        Objects.requireNonNull(redundantRows2, "redundantRows2");
        Objects.requireNonNull(redundantColumns1, "redundantColumns1");
        Objects.requireNonNull(redundantColumns2, "redundantColumns2");
        Objects.requireNonNull(diffCells, "diffCells");
        if (!considerRowGaps && (!redundantRows1.isEmpty() || !redundantRows2.isEmpty())) {
            throw new IllegalArgumentException("illegal row result");
        }
        if (!considerColumnGaps && (!redundantColumns1.isEmpty() || !redundantColumns2.isEmpty())) {
            throw new IllegalArgumentException("illegal column result");
        }
        
        return new SResult<>(
                considerRowGaps,
                considerColumnGaps,
                redundantRows1,
                redundantRows2,
                redundantColumns1,
                redundantColumns2,
                diffCells);
    }
    
    // [instance members] ******************************************************
    
    private final boolean considerRowGaps;
    private final boolean considerColumnGaps;
    private final Pair<List<Integer>> redundantRows;
    private final Pair<List<Integer>> redundantColumns;
    private final List<Pair<CellReplica<T>>> diffCells;
    
    private SResult(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            List<Integer> redundantRows1,
            List<Integer> redundantRows2,
            List<Integer> redundantColumns1,
            List<Integer> redundantColumns2,
            List<Pair<CellReplica<T>>> diffCells) {
        
        assert redundantRows1 != null;
        assert redundantRows2 != null;
        assert redundantColumns1 != null;
        assert redundantColumns2 != null;
        assert diffCells != null;
        
        assert considerRowGaps || redundantRows1.isEmpty();
        assert considerRowGaps || redundantRows2.isEmpty();
        assert considerColumnGaps || redundantColumns1.isEmpty();
        assert considerColumnGaps || redundantColumns2.isEmpty();
        
        this.considerRowGaps = considerRowGaps;
        this.considerColumnGaps = considerColumnGaps;
        
        // 一応、防御的コピーしておく。
        this.redundantRows = Pair.of(
                List.copyOf(redundantRows1),
                List.copyOf(redundantRows2));
        this.redundantColumns = Pair.of(
                List.copyOf(redundantColumns1),
                List.copyOf(redundantColumns2));
        this.diffCells = List.copyOf(diffCells);
    }
    
    /**
     * この比較において行の余剰／欠損が考慮されたかを返します。<br>
     * 
     * @return この比較において行の余剰／欠損が考慮された場合は {@code true}
     */
    public boolean considerRowGaps() {
        return considerRowGaps;
    }
    
    /**
     * この比較において列の余剰／欠損が考慮されたかを返します。<br>
     * 
     * @return この比較において列の余剰／欠損が考慮された場合は {@code true}
     */
    public boolean considerColumnGaps() {
        return considerColumnGaps;
    }
    
    /**
     * 余剰行のインデックスを返します。<br>
     * 
     * @return 余剰行のインデックス
     */
    public Pair<List<Integer>> reundantRows() {
        // 不変なのでこのまま返しちゃって問題ない。
        return redundantRows;
    }
    
    /**
     * 余剰列のインデックスを返します。<br>
     * 
     * @return 余剰列のインデックス
     */
    public Pair<List<Integer>> reundantColumns() {
        // 不変なのでこのまま返しちゃって問題ない。
        return redundantColumns;
    }
    
    /**
     * 差分セルを返します。<br>
     * 
     * @return 差分セル
     */
    public List<Pair<CellReplica<T>>> diffCells() {
        // 不変なのでこのまま返しちゃって問題ない。
        return diffCells;
    }
    
    /**
     * 指定された側のシートに関する差分内容を返します。<br>
     * 
     * @param side シートの側
     * @return 指定された側のシートに関する差分内容
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public Piece<T> getPiece(Side side) {
        Objects.requireNonNull(side, "side");
        
        return new Piece<>(
                redundantRows.get(side),
                redundantColumns.get(side),
                diffCells.stream().map(p -> p.get(side)).collect(Collectors.toList()));
    }
    
    /**
     * 比較結果のサマリを返します。<br>
     * 
     * @return 比較結果のサマリ
     */
    public String getSummary() {
        StringBuilder str = new StringBuilder();
        
        if (considerRowGaps) {
            str.append(String.format(
                    "余剰行 : シートA - %s, シートB - %s",
                    redundantRows.a().isEmpty() ? "(なし)" : redundantRows.a().size() + "行",
                    redundantRows.b().isEmpty() ? "(なし)" : redundantRows.b().size() + "行"))
                    .append(BR);
        }
        if (considerColumnGaps) {
            str.append(String.format(
                    "余剰列 : シートA - %s, シートB - %s",
                    redundantColumns.a().isEmpty() ? "(なし)" : redundantColumns.a().size() + "列",
                    redundantColumns.b().isEmpty() ? "(なし)" : redundantColumns.b().size() + "列"))
                    .append(BR);
        }
        str.append(String.format(
                "差分セル : %s",
                diffCells.isEmpty() ? "(なし)" : "各シート" + diffCells.size() + "セル"))
                .append(BR);
        
        return str.toString();
    }
    
    /**
     * 比較結果の詳細を返します。<br>
     * 
     * @return 比較結果の詳細
     */
    public String getDetail() {
        StringBuilder str = new StringBuilder();
        
        if (considerRowGaps) {
            for (Side side : Side.values()) {
                str.append(String.format("シート%s上の余剰行 : ", side)).append(BR);
                if (redundantRows.get(side).isEmpty()) {
                    str.append("    (なし)").append(BR);
                } else {
                    redundantRows.get(side).forEach(
                            row -> str.append("    行").append(row + 1).append(BR));
                }
            }
            str.append(BR);
        }
        if (considerColumnGaps) {
            for (Side side : Side.values()) {
                str.append(String.format("シート%s上の余剰列 : ", side)).append(BR);
                if (redundantColumns.get(side).isEmpty()) {
                    str.append("    (なし)").append(BR);
                } else {
                    redundantColumns.get(side).forEach(column -> str
                            .append("    ")
                            .append(CellReplica.columnIdxToStr(column))
                            .append("列").append(BR));
                }
            }
            str.append(BR);
        }
        str.append("差分セル : ");
        if (diffCells.isEmpty()) {
            str.append(BR).append("    (なし)").append(BR);
        } else {
            Iterator<Pair<CellReplica<T>>> itr = diffCells.iterator();
            while (itr.hasNext()) {
                Pair<CellReplica<T>> pair = itr.next();
                CellReplica<T> cell1 = pair.a();
                CellReplica<T> cell2 = pair.b();
                str.append(BR);
                str.append("    ").append(cell1).append(BR);
                str.append("    ").append(cell2).append(BR);
            }
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        return getDetail();
    }
    
    /**
     * 比較結果のコマンドライン出力用文字列を返します。<br>
     * 
     * @return 比較結果のコマンドライン出力用文字列
     */
    public String getDiff() {
        StringBuilder str = new StringBuilder();
        
        if (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty()) {
            str.append("Row Gaps :").append(BR);
            
            Function<List<Integer>, String> rowsToStr = rows -> rows.stream()
                    .map(i -> i + 1)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            
            if (!redundantRows.a().isEmpty()) {
                str.append("- ").append(rowsToStr.apply(redundantRows.a())).append(BR);
            }
            if (!redundantRows.b().isEmpty()) {
                str.append("+ ").append(rowsToStr.apply(redundantRows.b())).append(BR);
            }
            str.append(BR);
        }
        
        if (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty()) {
            str.append("Column Gaps :").append(BR);
            
            Function<List<Integer>, String> columnsToStr = columns -> columns.stream()
                    .map(CellReplica::columnIdxToStr)
                    .collect(Collectors.joining(", "));
            
            if (!redundantColumns.a().isEmpty()) {
                str.append("- ").append(columnsToStr.apply(redundantColumns.a())).append(BR);
            }
            if (!redundantColumns.b().isEmpty()) {
                str.append("+ ").append(columnsToStr.apply(redundantColumns.b())).append(BR);
            }
            str.append(BR);
        }
        
        if (!diffCells.isEmpty()) {
            str.append("Diff Cells :").append(BR);
            
            str.append(diffCells.stream()
                    .map(diffCell -> String.format("- %s\n+ %s\n", diffCell.a(), diffCell.b()))
                    .collect(Collectors.joining(BR)));
            
        }
        
        return str.toString();
    }
}
