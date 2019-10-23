/**
 * Excelブック同士、Excelシート同士の比較に関する各種機能を提供します。<br>
 * 
 * @author nmby
 */
module xyz.hotchpotch.hogandiff.excel {
    requires transitive xyz.hotchpotch.hogandiff.core;
    requires poi;
    requires java.xml;
    
    exports xyz.hotchpotch.hogandiff.excel;
}
