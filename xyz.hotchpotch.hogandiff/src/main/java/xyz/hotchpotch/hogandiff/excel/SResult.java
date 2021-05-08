package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelシート同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class SResult {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * 片側のシートに関する差分内容を表す不変クラスです。<br>
     *
     * @author nmby
     */
    public static record Piece(
            List<Integer> redundantRows,
            List<Integer> redundantColumns,
            List<CellReplica> diffCellContents,
            List<CellReplica> diffCellComments,
            List<CellReplica> redundantCellComments) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        // java16で正式導入されたRecordを使ってみたいが故にこのクラスをRecordとしているが、
        // 本来はコンストラクタを公開する必要がない。ぐぬぬ
        // recordを使う欲の方が上回ったのでコンストラクタを公開しちゃう。ぐぬぬ
        public Piece {
            Objects.requireNonNull(redundantRows, "redundantRows");
            Objects.requireNonNull(redundantColumns, "redundantColumns");
            Objects.requireNonNull(diffCellContents, "diffCellContents");
            Objects.requireNonNull(diffCellComments, "diffCellComments");
            Objects.requireNonNull(redundantCellComments, "redundantCellComments");
            
            // 一応防御的コピーしておく。
            redundantRows = List.copyOf(redundantRows);
            redundantColumns = List.copyOf(redundantColumns);
            diffCellContents = List.copyOf(diffCellContents);
            diffCellComments = List.copyOf(diffCellComments);
            redundantCellComments = List.copyOf(redundantCellComments);
        }
        
        /**
         * ひとつでも差分があるかを返します。<br>
         * 
         * @return ひとつでも差分がある場合は {@code true}
         */
        public boolean hasDiff() {
            return !redundantRows.isEmpty()
                    || !redundantColumns.isEmpty()
                    || !diffCellContents.isEmpty()
                    || !diffCellComments.isEmpty()
                    || !redundantCellComments.isEmpty();
        }
    }
    
    /**
     * Excelシート同士の比較結果を生成します。<br>
     * 
     * @param considerRowGaps 比較において行の余剰／欠損を考慮したか
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮したか
     * @param compareCellContents 比較においてセル内容を比較したか
     * @param compareCellComments 比較においてセルコメントを比較したか
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
    public static SResult of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean compareCellContents,
            boolean compareCellComments,
            List<Integer> redundantRows1,
            List<Integer> redundantRows2,
            List<Integer> redundantColumns1,
            List<Integer> redundantColumns2,
            List<Pair<CellReplica>> diffCells) {
        
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
        
        return new SResult(
                considerRowGaps,
                considerColumnGaps,
                compareCellContents,
                compareCellComments,
                redundantRows1,
                redundantRows2,
                redundantColumns1,
                redundantColumns2,
                diffCells);
    }
    
    // [instance members] ******************************************************
    
    private final boolean considerRowGaps;
    private final boolean considerColumnGaps;
    private final boolean compareCellContents;
    private final boolean compareCellComments;
    private final Pair<List<Integer>> redundantRows;
    private final Pair<List<Integer>> redundantColumns;
    private final List<Pair<CellReplica>> diffCells;
    private final List<Pair<CellReplica>> diffCellContents;
    private final List<Pair<CellReplica>> diffCellComments;
    private final Pair<List<CellReplica>> redundantCellComments;
    
    private SResult(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean compareCellContents,
            boolean compareCellComments,
            List<Integer> redundantRows1,
            List<Integer> redundantRows2,
            List<Integer> redundantColumns1,
            List<Integer> redundantColumns2,
            List<Pair<CellReplica>> diffCells) {
        
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
        this.compareCellContents = compareCellContents;
        this.compareCellComments = compareCellComments;
        
        // 一応、防御的コピーしておく。
        this.redundantRows = Pair.of(
                List.copyOf(redundantRows1),
                List.copyOf(redundantRows2));
        this.redundantColumns = Pair.of(
                List.copyOf(redundantColumns1),
                List.copyOf(redundantColumns2));
        this.diffCells = List.copyOf(diffCells);
        
        this.diffCellContents = !compareCellContents
                ? List.of()
                : List.copyOf(diffCells.stream()
                        .filter(p -> !Objects.equals(p.a().content(), p.b().content()))
                        .toList());
        this.diffCellComments = !compareCellComments
                ? List.of()
                : List.copyOf(diffCells.stream()
                        .filter(p -> p.a().comment() != null && p.b().comment() != null)
                        .filter(p -> !Objects.equals(p.a().comment(), p.b().comment()))
                        .toList());
        this.redundantCellComments = !compareCellComments
                ? Pair.of(List.of(), List.of())
                : Pair.of(
                        List.copyOf(diffCells.stream()
                                .filter(p -> p.a().comment() != null && p.b().comment() == null)
                                .map(Pair::a)
                                .toList()),
                        List.copyOf(diffCells.stream()
                                .filter(p -> p.a().comment() == null && p.b().comment() != null)
                                .map(Pair::b)
                                .toList()));
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
     * この比較においてセル内容が比較されたかを返します。<br>
     * 
     * @return この比較においてセル内容が比較された場合は {@code true}
     */
    public boolean compareCellContents() {
        return compareCellContents;
    }
    
    /**
     * この比較においてセルコメントが比較されたかを返します。<br>
     * 
     * @return この比較においてセルコメントが比較された場合は {@code true}
     */
    public boolean compareCellComments() {
        return compareCellComments;
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
     * セル内容の異なるセルを返します。<br>
     * 
     * @return セル内容の異なるセル
     */
    public List<Pair<CellReplica>> diffCellContents() {
        // 不変なのでこのまま返しちゃって問題ない。
        return diffCellContents;
    }
    
    /**
     * セルコメントの異なるセルを返します。<br>
     * 
     * @return セルコメントの異なるセル
     */
    public List<Pair<CellReplica>> diffCellComments() {
        // 不変なのでこのまま返しちゃって問題ない。
        return diffCellComments;
    }
    
    /**
     * 余剰セルコメントのセルを返します。<br>
     * 
     * @return 余剰セルコメントのセル
     */
    public Pair<List<CellReplica>> redundantCellComments() {
        // 不変なのでこのまま返しちゃって問題ない。
        return redundantCellComments;
    }
    
    /**
     * 指定された側のシートに関する差分内容を返します。<br>
     * 
     * @param side シートの側
     * @return 指定された側のシートに関する差分内容
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public Piece getPiece(Side side) {
        Objects.requireNonNull(side, "side");
        
        return new Piece(
                redundantRows.get(side),
                redundantColumns.get(side),
                diffCellContents.stream().map(p -> p.get(side)).toList(),
                diffCellComments.stream().map(p -> p.get(side)).toList(),
                redundantCellComments.get(side));
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return !redundantRows.a().isEmpty()
                || !redundantRows.b().isEmpty()
                || !redundantColumns.a().isEmpty()
                || !redundantColumns.b().isEmpty()
                || !diffCells.isEmpty();
    }
    
    /**
     * 比較結果の差分サマリを返します。<br>
     * 
     * @return 比較結果の差分サマリ
     */
    public String getDiffSummary() {
        if (!hasDiff()) {
            return "(差分なし)";
        }
        
        int rows = redundantRows.a().size() + redundantRows.b().size();
        int cols = redundantColumns.a().size() + redundantColumns.b().size();
        int cells = diffCells.size();
        
        StringBuilder str = new StringBuilder();
        if (0 < rows) {
            str.append("余剰行").append(rows);
        }
        if (0 < cols) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append("余剰列").append(cols);
        }
        if (0 < cells) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append("差分セル").append(cells);
        }
        
        return str.toString();
    }
    
    /**
     * 比較結果の差分詳細を返します。<br>
     * 
     * @return 比較結果の差分詳細
     */
    public String getDiffDetail() {
        if (!hasDiff()) {
            return "(差分なし)";
        }
        
        StringBuilder str = new StringBuilder();
        
        if (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> rows = redundantRows.get(side);
                if (!rows.isEmpty()) {
                    str.append(String.format("シート%s上の余剰行 : ", side)).append(BR);
                    rows.forEach(row -> str.append("    行").append(row + 1).append(BR));
                }
            }
            str.append(BR);
        }
        if (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> cols = redundantColumns.get(side);
                if (!cols.isEmpty()) {
                    str.append(String.format("シート%s上の余剰列 : ", side)).append(BR);
                    cols.forEach(column -> str
                            .append("    ")
                            .append(CellReplica.columnIdxToStr(column))
                            .append("列").append(BR));
                }
            }
            str.append(BR);
        }
        if (!diffCells.isEmpty()) {
            str.append("差分セル : ");
            diffCells.forEach(pair -> {
                str.append(BR);
                str.append("    ").append(pair.a()).append(BR);
                str.append("    ").append(pair.b()).append(BR);
            });
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        return getDiffDetail();
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
