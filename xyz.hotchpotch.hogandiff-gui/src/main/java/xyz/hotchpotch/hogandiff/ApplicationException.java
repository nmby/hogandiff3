package xyz.hotchpotch.hogandiff;

/**
 * アプリケーション処理に失敗したことを表す例外です。<br>
 *
 * @author nmby
 */
public class ApplicationException extends Exception {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 新しい例外を生成します。<br>
     */
    public ApplicationException() {
        super();
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     */
    public ApplicationException(String message) {
        super(message);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param cause 原因
     */
    public ApplicationException(Throwable cause) {
        super(cause);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     * @param cause 原因
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
