package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Supplier} のチェック例外をスローできるバージョンです。<br>
 * つまり、{@link Supplier#get()} はチェック例外をスローできませんが、
 * {@link UnsafeSupplier#get()} はスローできます。<br>
 *
 * @param <T> 生成される値の型
 * @author nmby
 * @see Supplier
 */
@FunctionalInterface
public interface UnsafeSupplier<T> {
    
    // [static members] ********************************************************
    
    /**
     * {@link Supplier} を {@link UnsafeSupplier} に変換します。<br>
     * 
     * @param <T> 生成される値の型
     * @param safer サプライヤ
     * @return 型だけが変換されたサプライヤ
     * @throws NullPointerException {@code safer} が {@code null} の場合
     */
    public static <T> UnsafeSupplier<T> from(Supplier<T> safer) {
        Objects.requireNonNull(safer, "safer");
        
        return safer::get;
    }
    
    /**
     * {@link UnsafeSupplier} を {@link Supplier} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @param <T> 生成される値の型
     * @param unsafer サプライヤ
     * @return スローする例外が変換されたサプライヤ
     * @throws NullPointerException {@code unsafer} が {@code null} の場合
     */
    public static <T> Supplier<T> toSupplier(UnsafeSupplier<T> unsafer) {
        return toSupplier(unsafer, RuntimeException::new);
    }
    
    /**
     * {@link UnsafeSupplier} を {@link Supplier} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param <T> 生成される値の型
     * @param unsafer サプライヤ
     * @param wrapper チェック例外を非チェック例外に変換するラッパー
     * @return スローする例外が変換されたサプライヤ
     * @throws NullPointerException {@code unsafer}, {@code wrapper} のいずれかが {@code null} の場合
     */
    public static <T> Supplier<T> toSupplier(
            UnsafeSupplier<T> unsafer,
            Function<? super Exception, ? extends RuntimeException> wrapper) {
        
        Objects.requireNonNull(unsafer, "unsafer");
        Objects.requireNonNull(wrapper, "wrapper");
        
        return () -> {
            try {
                return unsafer.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * {@link Supplier#get} の {@link Exception} をスローできるバージョンです。<br>
     * 値を取得します。<br>
     * 
     * @return 出力値
     * @throws Exception 何らかのチェック例外
     */
    T get() throws Exception;
}
