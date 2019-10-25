package xyz.hotchpotch.hogandiff.excel;

/**
 * Excelシートの種類を表す列挙型です。<br>
 * 
 * @author nmby
 */
public enum SheetType {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /** ワークシート */
    WORKSHEET("ワークシート"),
    
    /** グラフシート */
    CHART_SHEET("グラフシート"),
    
    /** MS Excel 5.0 ダイアログシート */
    DIALOG_SHEET("MS Excel 5.0 ダイアログシート"),
    
    /** Excel 4.0 マクロシート */
    MACRO_SHEET("Excel 4.0 マクロシート");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final String description;
    
    private SheetType(String description) {
        assert description != null;
        
        this.description = description;
    }
    
    /**
     * このシート種別の説明を返します。<br>
     * 
     * @return このシート種別の説明
     */
    public String description() {
        return description;
    }
}
