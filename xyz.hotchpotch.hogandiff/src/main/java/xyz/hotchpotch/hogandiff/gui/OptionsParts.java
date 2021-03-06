package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

public class OptionsParts extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private CheckBox considerRowGapsCheckBox;
    
    @FXML
    private CheckBox considerColumnGapsCheckBox;
    
    @FXML
    private CheckBox compareCellContentsCheckBox;
    
    @FXML
    private RadioButton compareOnValueRadioButton;
    
    @FXML
    private RadioButton compareOnFormulaRadioButton;
    
    @FXML
    private CheckBox compareCellCommentsCheckBox;
    
    @FXML
    private CheckBox showPaintedSheetsCheckBox;
    
    @FXML
    private CheckBox showResultTextCheckBox;
    
    @FXML
    private CheckBox exitWhenFinishedCheckBox;
    
    private final BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
    
    public OptionsParts() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionsParts.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public void init() {
        considerRowGapsCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        considerColumnGapsCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        compareCellContentsCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        compareOnValueRadioButton.setOnAction(event -> hasSettingsChanged.set(true));
        compareOnFormulaRadioButton.setOnAction(event -> hasSettingsChanged.set(true));
        compareCellCommentsCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        showPaintedSheetsCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        showResultTextCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        exitWhenFinishedCheckBox.setOnAction(event -> hasSettingsChanged.set(true));
        
        // 「セル内容を比較する」が選択された場合のみ、「値／数式」の選択を有効にする。
        compareOnValueRadioButton.disableProperty().bind(
                compareCellContentsCheckBox.selectedProperty().not());
        compareOnFormulaRadioButton.disableProperty().bind(
                compareCellContentsCheckBox.selectedProperty().not());
    }
    
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.CONSIDER_ROW_GAPS)) {
            considerRowGapsCheckBox.setSelected(settings.get(SettingKeys.CONSIDER_ROW_GAPS));
        }
        if (settings.containsKey(SettingKeys.CONSIDER_COLUMN_GAPS)) {
            considerColumnGapsCheckBox.setSelected(settings.get(SettingKeys.CONSIDER_COLUMN_GAPS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_CONTENTS)) {
            compareCellContentsCheckBox.setSelected(settings.get(SettingKeys.COMPARE_CELL_CONTENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_COMMENTS)) {
            compareCellCommentsCheckBox.setSelected(settings.get(SettingKeys.COMPARE_CELL_COMMENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_ON_FORMULA_STRING)) {
            compareOnFormulaRadioButton.setSelected(settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING));
        }
        if (settings.containsKey(SettingKeys.SHOW_PAINTED_SHEETS)) {
            showPaintedSheetsCheckBox.setSelected(settings.get(SettingKeys.SHOW_PAINTED_SHEETS));
        }
        if (settings.containsKey(SettingKeys.SHOW_RESULT_TEXT)) {
            showResultTextCheckBox.setSelected(settings.get(SettingKeys.SHOW_RESULT_TEXT));
        }
        if (settings.containsKey(SettingKeys.EXIT_WHEN_FINISHED)) {
            exitWhenFinishedCheckBox.setSelected(settings.get(SettingKeys.EXIT_WHEN_FINISHED));
        }
    }
    
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        builder.set(SettingKeys.CONSIDER_ROW_GAPS, considerRowGapsCheckBox.isSelected());
        builder.set(SettingKeys.CONSIDER_COLUMN_GAPS, considerColumnGapsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_CONTENTS, compareCellContentsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_COMMENTS, compareCellCommentsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_ON_FORMULA_STRING, compareOnFormulaRadioButton.isSelected());
        builder.set(SettingKeys.SHOW_PAINTED_SHEETS, showPaintedSheetsCheckBox.isSelected());
        builder.set(SettingKeys.SHOW_RESULT_TEXT, showResultTextCheckBox.isSelected());
        builder.set(SettingKeys.EXIT_WHEN_FINISHED, exitWhenFinishedCheckBox.isSelected());
        
        builder.setDefaultValue(SettingKeys.REDUNDANT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.SAME_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.WORK_DIR_BASE);
        builder.setDefaultValue(SettingKeys.CURR_TIMESTAMP);
    }
    
    public BooleanProperty hasSettingsChangedProperty() {
        return hasSettingsChanged;
    }
}
