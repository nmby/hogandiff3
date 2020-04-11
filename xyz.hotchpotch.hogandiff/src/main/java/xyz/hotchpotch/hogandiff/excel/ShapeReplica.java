package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

/**
 * Excelシート上のオートシェイプ（図形）を表します。<br>
 * 
 * @author nmby
 */
public class ShapeReplica {
    
    // [static members] ********************************************************
    
    /**
     * 新たなシェイプレプリカを生成します。<br>
     * 
     * @param id シェイプのid
     * @param text シェイプ内のテキスト（{@code null} 許容）
     * @return 新たなシェイプレプリカ
     */
    public static ShapeReplica of(int id, String text) {
        return new ShapeReplica(id, text);
    }
    
    // [instance members] ******************************************************
    
    private final int id;
    private final String text;
    
    private ShapeReplica(
            int id,
            String text) {
        
        this.id = id;
        this.text = text;
    }
    
    public int id() {
        return id;
    }
    
    public String text() {
        return text;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ShapeReplica) {
            ShapeReplica other = (ShapeReplica) o;
            return id == other.id && Objects.equals(text, other.text);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, text);
    }
    
    @Override
    public String toString() {
        return String.format("%d: %s", id, text);
    }
}
