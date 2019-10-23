/**
 * 方眼Diffアプリケーション（GUI）を提供します。<br>
 * 
 * @author nmby
 */
module xyz.hotchpotch.hogandiff.gui {
    requires xyz.hotchpotch.hogandiff.excel;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    
    opens xyz.hotchpotch.hogandiff to javafx.graphics, javafx.fxml;
}
