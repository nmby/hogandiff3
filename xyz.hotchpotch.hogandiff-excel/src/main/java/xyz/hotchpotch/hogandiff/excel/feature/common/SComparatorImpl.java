package xyz.hotchpotch.hogandiff.excel.feature.common;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.CellReplica.CellContentType;
import xyz.hotchpotch.hogandiff.excel.CellReplica.CellId;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link SComparator} の標準的な実装です。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
public class SComparatorImpl<T> implements SComparator<T> {
    
    // [static members] ********************************************************
    
    /**
     * 行同士または列同士の対応関係を決定するマッパーを表します。<br>
     * これは、{@link #makePairs(Set, Set)} を関数メソッドに持つ関数型インタフェースです。<br>
     *
     * @param <T> セルデータの型
     * @author nmby
     */
    @FunctionalInterface
    private static interface Mapper<T> {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * 行同士または列同士の対応関係を決定し、
         * インデックスのペアのリストとして返します、<br>
         * 
         * @param cells1 セルセット1
         * @param cells2 セルセット2
         * @return 行同士または列同士の対応関係
         */
        List<Pair<Integer>> makePairs(
                Set<CellReplica> cells1,
                Set<CellReplica> cells2);
    }
    
    /**
     *  縦方向の余剰／欠損を考慮しない場合のマッパーを返します。<br>
     * 
     * @param <T> セルデータの型
     * @param verticality 縦方向の座標を抽出する関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static <T> Mapper<T> mapper(
            ToIntFunction<CellReplica> verticality) {
        
        assert verticality != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            Pair<Integer> range = range(cells1, cells2, verticality);
            return IntStream.rangeClosed(range.a(), range.b())
                    .mapToObj(n -> Pair.of(n, n))
                    .collect(Collectors.toList());
        };
    }
    
    /**
     * 縦方向の余剰／欠損を考慮する場合のマッパーを返します。<br>
     * 
     * @param <T> セルデータの型
     * @param <U> 横方向のソートキーの型
     * @param verticality 縦方向の座標を抽出する関数
     * @param extractor 横方向のソートキーを抽出する関数
     * @param comparator 横方向のソートキーをソートするための比較関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static <T, U> Mapper<T> mapper(
            ToIntFunction<CellReplica> verticality,
            Function<CellReplica, ? extends U> extractor,
            Comparator<? super U> comparator,
            CellContentType<T> targetContentType) {
        
        assert verticality != null;
        assert extractor != null;
        assert comparator != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            int start = range(cells1, cells2, verticality).a();
            List<List<CellReplica>> cellsList1 = convert(
                    cells1, start, verticality, extractor, comparator);
            List<List<CellReplica>> cellsList2 = convert(
                    cells2, start, verticality, extractor, comparator);
            
            Matcher<List<CellReplica>> matcher = Matcher.minimumEditDistanceMatcherOf(
                    List::size,
                    (list1, list2) -> evaluateDiff(list1, list2, extractor, comparator, targetContentType));
            
            return matcher.makePairs(cellsList1, cellsList2).stream()
                    .map(p -> p.map(i -> i + start))
                    .collect(Collectors.toList());
        };
    }
    
    private static <T, U> int evaluateDiff(
            List<CellReplica> list1,
            List<CellReplica> list2,
            Function<CellReplica, ? extends U> extractor,
            Comparator<? super U> comparator,
            CellContentType<T> targetContentType) {
        
        assert list1 != null;
        assert list2 != null;
        assert list1 != list2;
        assert extractor != null;
        assert comparator != null;
        
        Iterator<CellReplica> itr1 = list1.iterator();
        Iterator<CellReplica> itr2 = list2.iterator();
        
        int diff = 0;
        int comp = 0;
        U key1 = null;
        U key2 = null;
        T value1 = null;
        T value2 = null;
        
        while (itr1.hasNext() && itr2.hasNext()) {
            if (comp <= 0) {
                CellReplica cell1 = itr1.next();
                key1 = extractor.apply(cell1);
                value1 = cell1.getContent(targetContentType);
            }
            if (0 <= comp) {
                CellReplica cell2 = itr2.next();
                key2 = extractor.apply(cell2);
                value2 = cell2.getContent(targetContentType);
            }
            comp = comparator.compare(key1, key2);
            if (comp == 0 && !Objects.equals(value1, value2)) {
                diff += 2;
            } else if (comp != 0) {
                diff++;
            }
        }
        while (itr1.hasNext()) {
            diff++;
            itr1.next();
        }
        while (itr2.hasNext()) {
            diff++;
            itr2.next();
        }
        
        return diff;
    }
    
    /**
     * セルのセットを、セルのリストのリストに変換します。<br>
     * 一次元目のリストは verticality でソートしたものであり、
     * 二次元目のリストは同一 verticality 値のものを
     * extractor と comparator でソートしたものです。<br>
     * 
     * @param <T> セルデータの型
     * @param <U> 横方向のソートキーの型
     * @param cells セルセット
     * @param start セルセットのリスト化を始める最小インデックス値
     * @param verticality 縦方向の値（行または列）の抽出関数
     * @param extractor 横方向のソートキーの抽出関数
     * @param comparator 横方向のソートキーの比較関数
     * @return セルのリストのリスト
     */
    private static <T, U> List<List<CellReplica>> convert(
            Set<CellReplica> cells,
            int start,
            ToIntFunction<CellReplica> verticality,
            Function<CellReplica, ? extends U> extractor,
            Comparator<? super U> comparator) {
        
        assert cells != null;
        assert 0 <= start;
        assert verticality != null;
        assert extractor != null;
        assert comparator != null;
        
        Map<Integer, List<CellReplica>> map = cells.stream()
                .collect(Collectors.groupingBy(verticality::applyAsInt));
        
        int end = range(cells, verticality).b();
        return IntStream.rangeClosed(start, end).parallel()
                .mapToObj(i -> {
                    if (map.containsKey(i)) {
                        List<CellReplica> list = map.get(i);
                        list.sort(Comparator.comparing(extractor, comparator));
                        return list;
                    } else {
                        return List.<CellReplica> of();
                    }
                })
                .collect(Collectors.toList());
    }
    
    private static <T> Pair<Integer> range(
            Set<CellReplica> cells,
            ToIntFunction<CellReplica> axis) {
        
        assert cells != null;
        assert axis != null;
        
        int min = cells.stream()
                .mapToInt(axis)
                .min().orElse(0);
        int max = cells.stream()
                .mapToInt(axis)
                .max().orElse(0);
        
        return Pair.of(min, max);
    }
    
    private static <T> Pair<Integer> range(
            Set<CellReplica> cells1,
            Set<CellReplica> cells2,
            ToIntFunction<CellReplica> axis) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert axis != null;
        
        Pair<Integer> range1 = range(cells1, axis);
        Pair<Integer> range2 = range(cells2, axis);
        
        return Pair.of(
                Math.min(range1.a(), range2.a()),
                Math.max(range1.b(), range2.b()));
    }
    
    /**
     * 新しいコンパレータを返します。<br>
     * 
     * @param <T> セルデータの型
     * @param considerRowGaps 比較において行の余剰／欠損を考慮する場合は {@code true}
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮する場合は {@code true}
     * @return 新しいコンパレータ
     */
    public static <T extends Comparable<? super T>> SComparator<T> of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            CellContentType<T> targetContentType) {
        
        return of(
                considerRowGaps,
                considerColumnGaps,
                Comparator.naturalOrder(),
                targetContentType);
    }
    
    /**
     * 新しいコンパレータを返します。<br>
     * 
     * @param <T> セルデータの型
     * @param considerRowGaps 比較において行の余剰／欠損を考慮する場合は {@code true}
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮する場合は {@code true}
     * @param dataComparator {@code T} 型オブジェクトの比較関数
     * @param targetContentType 対象とするセル内容物の種類
     * @return 新しいコンパレータ
     * @throws NullPointerException {@code dataComparator} が {@code null} の場合
     */
    public static <T> SComparator<T> of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            Comparator<? super T> dataComparator,
            CellContentType<T> targetContentType) {
        
        Objects.requireNonNull(dataComparator, "dataComparator");
        
        return new SComparatorImpl<>(
                considerRowGaps,
                considerColumnGaps,
                dataComparator,
                targetContentType);
    }
    
    // [instance members] ******************************************************
    
    private final boolean considerRowGaps;
    private final boolean considerColumnGaps;
    private final Mapper<T> rowsMapper;
    private final Mapper<T> columnsMapper;
    private final CellContentType<T> targetContentType;
    
    private SComparatorImpl(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            Comparator<? super T> dataComparator,
            CellContentType<T> targetContentType) {
        
        assert dataComparator != null;
        
        this.considerRowGaps = considerRowGaps;
        this.considerColumnGaps = considerColumnGaps;
        this.targetContentType = targetContentType;
        
        if (considerRowGaps && considerColumnGaps) {
            rowsMapper = mapper(c -> c.id().row(), c -> c.getContent(targetContentType), dataComparator, targetContentType);
            columnsMapper = mapper(c -> c.id().column(), c -> c.getContent(targetContentType), dataComparator, targetContentType);
        } else if (considerRowGaps) {
            rowsMapper = mapper(c -> c.id().row(), c -> c.id().column(), Comparator.naturalOrder(), targetContentType);
            columnsMapper = mapper(c -> c.id().column());
        } else if (considerColumnGaps) {
            rowsMapper = mapper(c -> c.id().row());
            columnsMapper = mapper(c -> c.id().column(), c -> c.id().row(), Comparator.naturalOrder(), targetContentType);
        } else {
            rowsMapper = mapper(c -> c.id().row());
            columnsMapper = mapper(c -> c.id().column());
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code cells1}, {@code cells2} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code cells1}, {@code cells2} が同一インスタンスの場合
     */
    @Override
    public SResult compare(
            Set<CellReplica> cells1,
            Set<CellReplica> cells2) {
        
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        if (cells1 == cells2) {
            throw new IllegalArgumentException("cells1 == cells2");
        }
        
        List<Pair<Integer>> rowPairs = rowsMapper.makePairs(cells1, cells2);
        List<Pair<Integer>> columnPairs = columnsMapper.makePairs(cells1, cells2);
        
        // 余剰行の収集
        List<Integer> redundantRows1 = rowPairs.stream()
                .filter(Pair::isOnlyA).map(Pair::a).collect(Collectors.toList());
        List<Integer> redundantRows2 = rowPairs.stream()
                .filter(Pair::isOnlyB).map(Pair::b).collect(Collectors.toList());
        
        // 余剰列の収集
        List<Integer> redundantColumns1 = columnPairs.stream()
                .filter(Pair::isOnlyA).map(Pair::a).collect(Collectors.toList());
        List<Integer> redundantColumns2 = columnPairs.stream()
                .filter(Pair::isOnlyB).map(Pair::b).collect(Collectors.toList());
        
        // 差分セルの収集
        List<Pair<CellReplica>> diffCells = extractDiffs(
                cells1, cells2, rowPairs, columnPairs);
        
        return SResult.of(
                considerRowGaps,
                considerColumnGaps,
                redundantRows1,
                redundantRows2,
                redundantColumns1,
                redundantColumns2,
                diffCells);
    }
    
    private List<Pair<CellReplica>> extractDiffs(
            Set<CellReplica> cells1,
            Set<CellReplica> cells2,
            List<Pair<Integer>> rowPairs,
            List<Pair<Integer>> columnPairs) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert rowPairs != null;
        assert columnPairs != null;
        
        Map<String, CellReplica> map1 = cells1.stream()
                .collect(Collectors.toMap(c -> c.id().address(), Function.identity()));
        Map<String, CellReplica> map2 = cells2.stream()
                .collect(Collectors.toMap(c -> c.id().address(), Function.identity()));
        
        return rowPairs.parallelStream().filter(Pair::isPaired).flatMap(rp -> {
            int row1 = rp.a();
            int row2 = rp.b();
            
            return columnPairs.stream().filter(Pair::isPaired).map(cp -> {
                int column1 = cp.a();
                int column2 = cp.b();
                String address1 = CellId.idxToAddress(row1, column1);
                String address2 = CellId.idxToAddress(row2, column2);
                CellReplica cell1 = map1.get(address1);
                CellReplica cell2 = map2.get(address2);
                T value1 = (cell1 == null ? null : cell1.getContent(targetContentType));
                T value2 = (cell2 == null ? null : cell2.getContent(targetContentType));
                
                return Objects.equals(value1, value2)
                        ? null
                        : Pair.<CellReplica> of(
                                cell1 != null ? cell1 : CellReplica.empty(row1, column1),
                                cell2 != null ? cell2 : CellReplica.empty(row2, column2));
            });
        }).filter(p -> p != null).collect(Collectors.toList());
    }
}
