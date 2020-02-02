package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * {@link Function} のチェック例外をスローできるバージョンです。<br>
 * つまり、{@link Function#apply(Object)} はチェック例外をスローできませんが、
 * {@link UnsafeFunction#apply(Object)} はスローできます。<br>
 *
 * @param <T> 入力の型
 * @param <R> 出力の型
 * @author nmby
 * @see Function
 */
@FunctionalInterface
public interface UnsafeFunction<T, R> {
    
    // [static members] ********************************************************
    
    /**
     * 常に入力引数を返す関数を返します。<br>
     * 
     * @param <T> 関数の入力および出力の型
     * @return 常に入力引数を返す関数
     */
    public static <T> UnsafeFunction<T, T> identity() {
        return t -> t;
    }
    
    /**
     * {@link Function} を {@link UnsafeFunction} に変換します。<br>
     * 
     * @param <T> 入力の型
     * @param <R> 出力の型
     * @param safer 関数
     * @return 型だけが変換された関数
     * @throws NullPointerException {@code safer} が {@code null} の場合
     */
    public static <T, R> UnsafeFunction<T, R> from(Function<T, R> safer) {
        Objects.requireNonNull(safer, "safer");
        
        return safer::apply;
    }
    
    /**
     * {@link UnsafeFunction} を {@link Function} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @param <T> 入力の型
     * @param <R> 出力の型
     * @param unsafer 関数
     * @return 型だけが変換された関数
     * @throws NullPointerException {@code unsafer} が {@code null} の場合
     */
    public static <T, R> Function<T, R> toFunction(UnsafeFunction<T, R> unsafer) {
        return toFunction(unsafer, RuntimeException::new);
    }
    
    /**
     * {@link UnsafeFunction} を {@link Function} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param <T> 入力の型
     * @param <R> 出力の型
     * @param unsafer 関数
     * @param wrapper チェック例外を非チェック例外に変換するラッパー
     * @return スローする例外が変換された関数
     * @throws NullPointerException {@code unsafer}, {@code wrapper} のいずれかが {@code null} の場合
     */
    public static <T, R> Function<T, R> toFunction(
            UnsafeFunction<T, R> unsafer,
            Function<? super Exception, ? extends RuntimeException> wrapper) {
        
        Objects.requireNonNull(unsafer, "unsafer");
        Objects.requireNonNull(wrapper, "wrapper");
        
        return t -> {
            try {
                return unsafer.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定された引数にこの関数を適用します。<br>
     * 
     * @param t 関数の引数
     * @return {@code t} 関数の結果
     * @throws Exception 何らかのチェック例外
     */
    R apply(T t) throws Exception;
    
    /**
     * {@link Function#compose(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code before} 関数および合成関数の入力の型
     * @param before この関数を適用する前に適用する関数
     * @return まず {@code before} を適用し、次にこの関数を適用する合成関数
     * @throws NullPointerException {@code before} が {@code null} の場合
     */
    default <V> UnsafeFunction<V, R> compose(UnsafeFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before, "before");
        
        return v -> apply(before.apply(v));
    }
    
    /**
     * {@link Function#compose(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code before} 関数および合成関数の入力の型
     * @param before この関数を適用する前に適用する関数
     * @return まず {@code before} を適用し、次にこの関数を適用する合成関数
     * @throws NullPointerException {@code before} が {@code null} の場合
     */
    default <V> UnsafeFunction<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before, "before");
        
        return v -> apply(before.apply(v));
    }
    
    /**
     * {@link Function#andThen(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code after} 関数および合成関数の出力の型
     * @param after この関数を適用した後に適用する関数
     * @return まずこの関数を適用し、次に {@code after} 関数を適用する合成関数
     * @throws NullPointerException {@code after} が {@code null} の場合
     */
    default <V> UnsafeFunction<T, V> andThen(UnsafeFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after");
        
        return t -> after.apply(apply(t));
    }
    
    /**
     * {@link Function#andThen(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code after} 関数および合成関数の出力の型
     * @param after この関数を適用した後に適用する関数
     * @return まずこの関数を適用し、次に {@code after} 関数を適用する合成関数
     * @throws NullPointerException {@code after} が {@code null} の場合
     */
    default <V> UnsafeFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after");
        
        return t -> after.apply(apply(t));
    }
}
