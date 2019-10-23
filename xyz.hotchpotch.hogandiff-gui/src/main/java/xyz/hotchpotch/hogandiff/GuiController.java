package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

/**
 * このアプリケーションのコントローラです。<br>
 *
 * @author nmby
 */
public class GuiController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    // 比較対象選択エリア -----------------------
    
    @FXML
    private Pane paneSelectMenu;
    
    @FXML
    private RadioButton radioCompareBooks;
    
    @FXML
    private RadioButton radioCompareSheets;
    
    @FXML
    private Pane paneSelectTargets;
    
    @FXML
    private TextField textBookPath1;
    
    @FXML
    private TextField textBookPath2;
    
    @FXML
    private Button buttonBookPath1;
    
    @FXML
    private Button buttonBookPath2;
    
    @FXML
    private Label labelSheetName1;
    
    @FXML
    private Label labelSheetName2;
    
    @FXML
    private ChoiceBox<String> choiceSheetName1;
    
    @FXML
    private ChoiceBox<String> choiceSheetName2;
    
    // 設定エリア ---------------------------
    
    @FXML
    private Pane paneSettings;
    
    @FXML
    private CheckBox checkConsiderRowGaps;
    
    @FXML
    private CheckBox checkConsiderColumnGaps;
    
    @FXML
    private RadioButton radioCompareOnValue;
    
    @FXML
    private RadioButton radioCompareOnFormula;
    
    @FXML
    private CheckBox checkShowPaintedSheets;
    
    @FXML
    private CheckBox checkShowResultText;
    
    @FXML
    private CheckBox checkExitWhenFinished;
    
    @FXML
    private Button buttonSaveSettings;
    
    @FXML
    private Button buttonExecute;
    
    // レポートエリア -------------------------
    
    @FXML
    private Pane paneReporting;
    
    @FXML
    private ProgressBar progressReport;
    
    @FXML
    private TextArea textReport;
    
    // Utilエリア -------------------------
    
    @FXML
    private Pane paneUtil;
    
    @FXML
    private Button buttonShowWorkDir;
    
    @FXML
    private Button buttonDeleteOldWorkDir;
    
    @FXML
    private Hyperlink linkToWebSite;
    
    // その他プロパティ --------------------------
    
    private Property<AppMenu> menu = new SimpleObjectProperty<>();
    private Property<Path> bookPath1 = new SimpleObjectProperty<>();
    private Property<Path> bookPath2 = new SimpleObjectProperty<>();
    private StringProperty sheetName1 = new SimpleStringProperty();
    private StringProperty sheetName2 = new SimpleStringProperty();
    private BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
    private BooleanProperty isReady = new SimpleBooleanProperty(false);
    private BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    // その他メンバ --------------------------
    
    private Path prevSelectedBookPath;
    private Factory<?> factory;
    
    /**
     * 実行の準備が整っているかを返します。<br>
     * 
     * @return 実行の準備が整っている場合は {@code true}
     */
    public boolean isReady() {
        return isReady.getValue();
    }
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        factory = Factory.basicFactoryOf();
        
        initProperties();
        initTargetSelectionArea();
        initSettingsArea();
        initExecutionArea();
        initUtilArea();
    }
    
    private void initProperties() {
        // 比較メニューの選択状態を反映させる。
        menu.bind(Bindings.createObjectBinding(
                () -> radioCompareBooks.isSelected()
                        ? AppMenu.COMPARE_BOOKS
                        : AppMenu.COMPARE_SHEETS,
                radioCompareBooks.selectedProperty()));
        
        // シート名選択プルダウンの選択内容を反映させる。
        sheetName1.bind(choiceSheetName1.valueProperty());
        sheetName2.bind(choiceSheetName2.valueProperty());
        
        // 各種コントローラの設定状況に応じて「実行」可能な状態か否かを反映させる。
        isReady.bind(Bindings.createBooleanBinding(
                () -> bookPath1.getValue() != null
                        && bookPath2.getValue() != null
                        && (menu.getValue() == AppMenu.COMPARE_BOOKS
                                || (sheetName1.getValue() != null && sheetName2.getValue() != null)),
                menu, bookPath1, bookPath2, sheetName1, sheetName2));
        
        // 以下のプロパティについては、バインディングで値を反映させるのではなく
        // 相手方のイベントハンドラで値を設定する。
        //      ・bookPath1, bookPath2
        //      ・prevSelectedBookPath
        //      ・hasSettingsChanged
        //      ・isRunning
    }
    
    private void initTargetSelectionArea() {
        // 比較メニューの選択に応じて有効／無効を切り替える。
        labelSheetName1.disableProperty().bind(radioCompareSheets.selectedProperty().not());
        labelSheetName2.disableProperty().bind(radioCompareSheets.selectedProperty().not());
        choiceSheetName1.disableProperty().bind(radioCompareSheets.selectedProperty().not());
        choiceSheetName2.disableProperty().bind(radioCompareSheets.selectedProperty().not());
        
        // ファイルの指定内容に応じてブックパス表示を切り替える。
        textBookPath1.textProperty().bind(Bindings.createStringBinding(
                () -> bookPath1.getValue() == null ? "" : bookPath1.getValue().toString(),
                bookPath1));
        textBookPath2.textProperty().bind(Bindings.createStringBinding(
                () -> bookPath2.getValue() == null ? "" : bookPath2.getValue().toString(),
                bookPath2));
        
        // ファイルの指定内容に応じてシート名選択プルダウンの選択肢を切り替える。
        choiceSheetName1.itemsProperty().bind(Bindings.createObjectBinding(
                () -> getSheetNames(bookPath1.getValue()),
                bookPath1));
        choiceSheetName2.itemsProperty().bind(Bindings.createObjectBinding(
                () -> getSheetNames(bookPath2.getValue()),
                bookPath2));
        
        // ファイル選択ボタンのイベントハンドラを登録する。
        buttonBookPath1.setOnAction(event -> bookPath1.setValue(selectBook(bookPath1.getValue())));
        buttonBookPath2.setOnAction(event -> bookPath2.setValue(selectBook(bookPath2.getValue())));
        
        // ファイルパス表示テキストのドラッグ＆ドロップイベントハンドラを登録する。
        textBookPath1.setOnDragOver(this::onDragOver);
        textBookPath2.setOnDragOver(this::onDragOver);
        textBookPath1.setOnDragDropped(event -> onDragDropped(event, bookPath1));
        textBookPath2.setOnDragDropped(event -> onDragDropped(event, bookPath2));
    }
    
    private void initSettingsArea() {
        // 各種設定を変更した場合は、それをプロパティに反映させる。
        checkConsiderRowGaps.setOnAction(event -> hasSettingsChanged.set(true));
        checkConsiderColumnGaps.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnValue.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnFormula.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowPaintedSheets.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowResultText.setOnAction(event -> hasSettingsChanged.set(true));
        checkExitWhenFinished.setOnAction(event -> hasSettingsChanged.set(true));
        
        // 各種設定の変更有無に応じて「設定の保存」ボタンの有効／無効を切り替える。
        buttonSaveSettings.disableProperty().bind(hasSettingsChanged.not());
        
        // 「設定を保存」ボタンのイベントハンドラを登録する。
        buttonSaveSettings.setOnAction(event -> {
            Settings settings = gatherSettings(AppMain.keysToBeSaved);
            Properties properties = settings.toProperties();
            AppMain.storeProperties(properties);
            hasSettingsChanged.set(false);
        });
    }
    
    private void initExecutionArea() {
        // 各種設定状況に応じて「実行」ボタンの有効／無効を切り替える。
        buttonExecute.disableProperty().bind(isReady.not());
        
        // 実行中は全コントローラを無効にする。
        paneSelectMenu.disableProperty().bind(isRunning);
        paneSelectTargets.disableProperty().bind(isRunning);
        paneSettings.disableProperty().bind(isRunning);
        paneUtil.disableProperty().bind(isRunning);
        
        // レポートエリアは常に有効にすることにする。
        //paneReporting.disableProperty().bind(isRunning.not());
        
        // 「実行」ボタンのイベントハンドラを登録する。
        buttonExecute.setOnAction(event -> execute());
    }
    
    private void initUtilArea() {
        Path workDir = AppSettingKeys.WORK_DIR_BASE.defaultValueSupplier().get();
        
        buttonShowWorkDir.setOnAction(event -> {
            try {
                if (!Files.isDirectory(workDir)) {
                    Files.createDirectories(workDir);
                }
                Desktop.getDesktop().open(workDir.toFile());
            } catch (Exception e) {
                // nop
            }
        });
        
        buttonDeleteOldWorkDir.setOnAction(event -> {
            try (Stream<Path> children = Files.walk(workDir)) {
                children.filter(path -> !path.equals(workDir))
                        .sorted(Comparator.reverseOrder())
                        .forEach(UnsafeConsumer.toConsumer(Files::deleteIfExists));
            } catch (Exception e) {
                //nop
            }
        });
        
        linkToWebSite.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://hogandiff.hotchpotch.xyz/"));
            } catch (Exception e) {
                // nop
            }
        });
    }
    
    /**
     * 指定された設定の内容で各種コントローラの状態を変更します。<br>
     * 
     * @param settings 設定
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        settings.keySet().forEach(key -> {
            if (key == AppSettingKeys.CURR_MENU) {
                radioCompareBooks.setSelected(settings.get(key) == AppMenu.COMPARE_BOOKS);
                
            } else if (key == AppSettingKeys.CURR_BOOK_PATH1) {
                bookPath1.setValue((Path) settings.get(key));
                prevSelectedBookPath = (Path) settings.get(key);
                
            } else if (key == AppSettingKeys.CURR_BOOK_PATH2) {
                bookPath2.setValue((Path) settings.get(key));
                prevSelectedBookPath = (Path) settings.get(key);
                
            } else if (key == AppSettingKeys.CURR_SHEET_NAME1) {
                choiceSheetName1.setValue((String) settings.get(key));
                
            } else if (key == AppSettingKeys.CURR_SHEET_NAME2) {
                choiceSheetName2.setValue((String) settings.get(key));
                
            } else if (key == SettingKeys.CONSIDER_ROW_GAPS) {
                checkConsiderRowGaps.setSelected((boolean) settings.get(key));
                
            } else if (key == SettingKeys.CONSIDER_COLUMN_GAPS) {
                checkConsiderColumnGaps.setSelected((boolean) settings.get(key));
                
            } else if (key == SettingKeys.COMPARE_ON_FORMULA_STRING) {
                radioCompareOnFormula.setSelected((boolean) settings.get(key));
                
            } else if (key == AppSettingKeys.SHOW_PAINTED_SHEETS) {
                checkShowPaintedSheets.setSelected((boolean) settings.get(key));
                
            } else if (key == AppSettingKeys.SHOW_RESULT_TEXT) {
                checkShowResultText.setSelected((boolean) settings.get(key));
                
            } else if (key == AppSettingKeys.EXIT_WHEN_FINISHED) {
                checkExitWhenFinished.setSelected((boolean) settings.get(key));
            }
        });
    }
    
    private ObservableList<String> getSheetNames(Path bookPath) {
        if (bookPath == null) {
            return FXCollections.emptyObservableList();
        }
        try {
            BookLoader loader = factory.bookLoader(bookPath);
            List<String> sheetNames = loader.loadSheetNames(bookPath);
            return FXCollections.observableList(sheetNames);
        } catch (Exception e) {
            return FXCollections.emptyObservableList();
        }
    }
    
    private Path selectBook(Path current) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("比較対象ブックの選択");
        
        if (current != null) {
            chooser.setInitialDirectory(current.toFile().getParentFile());
            chooser.setInitialFileName(current.toFile().getName());
        } else if (prevSelectedBookPath != null) {
            chooser.setInitialDirectory(prevSelectedBookPath.toFile().getParentFile());
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Excel ブック", "*.xls", "*.xlsx", "*.xlsm"));
        
        File selected = chooser.showOpenDialog(paneSelectTargets.getScene().getWindow());
        
        return selected != null
                ? (prevSelectedBookPath = selected.toPath())
                : current;
    }
    
    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.LINK);
        }
        event.consume();
    }
    
    private void onDragDropped(DragEvent event, Property<Path> target) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            Path dropped = db.getFiles().get(0).toPath();
            if (!Files.isDirectory(dropped)) {
                target.setValue(dropped);
                prevSelectedBookPath = dropped;
            }
        }
        event.setDropCompleted(db.hasFiles());
        event.consume();
    }
    
    private Settings gatherSettings(Set<Settings.Key<?>> targets) {
        Settings.Builder builder = Settings.builder();
        
        // なんかもうちょいスマートに出来そうな気もするけど。。。
        // このクラスはやっつけで良いやと思ってしまう。。。
        if (targets == null || targets.contains(AppSettingKeys.CURR_MENU)) {
            builder.set(AppSettingKeys.CURR_MENU, menu.getValue());
        }
        if (targets == null || targets.contains(AppSettingKeys.CURR_BOOK_PATH1)) {
            builder.set(AppSettingKeys.CURR_BOOK_PATH1, bookPath1.getValue());
        }
        if (targets == null || targets.contains(AppSettingKeys.CURR_BOOK_PATH2)) {
            builder.set(AppSettingKeys.CURR_BOOK_PATH2, bookPath2.getValue());
        }
        if (menu.getValue() == AppMenu.COMPARE_SHEETS) {
            if (targets == null || targets.contains(AppSettingKeys.CURR_SHEET_NAME1)) {
                builder.set(AppSettingKeys.CURR_SHEET_NAME1, sheetName1.getValue());
            }
            if (targets == null || targets.contains(AppSettingKeys.CURR_SHEET_NAME2)) {
                builder.set(AppSettingKeys.CURR_SHEET_NAME2, sheetName2.getValue());
            }
        }
        if (targets == null || targets.contains(SettingKeys.CONSIDER_ROW_GAPS)) {
            builder.set(SettingKeys.CONSIDER_ROW_GAPS, checkConsiderRowGaps.isSelected());
        }
        if (targets == null || targets.contains(SettingKeys.CONSIDER_COLUMN_GAPS)) {
            builder.set(SettingKeys.CONSIDER_COLUMN_GAPS, checkConsiderColumnGaps.isSelected());
        }
        if (targets == null || targets.contains(SettingKeys.COMPARE_ON_FORMULA_STRING)) {
            builder.set(SettingKeys.COMPARE_ON_FORMULA_STRING, radioCompareOnFormula.isSelected());
        }
        if (targets == null || targets.contains(AppSettingKeys.SHOW_PAINTED_SHEETS)) {
            builder.set(AppSettingKeys.SHOW_PAINTED_SHEETS, checkShowPaintedSheets.isSelected());
        }
        if (targets == null || targets.contains(AppSettingKeys.SHOW_RESULT_TEXT)) {
            builder.set(AppSettingKeys.SHOW_RESULT_TEXT, checkShowResultText.isSelected());
        }
        if (targets == null || targets.contains(AppSettingKeys.EXIT_WHEN_FINISHED)) {
            builder.set(AppSettingKeys.EXIT_WHEN_FINISHED, checkExitWhenFinished.isSelected());
        }
        if (targets == null || targets.contains(SettingKeys.REDUNDANT_COLOR)) {
            builder.setDefaultValue(SettingKeys.REDUNDANT_COLOR);
        }
        if (targets == null || targets.contains(SettingKeys.DIFF_COLOR)) {
            builder.setDefaultValue(SettingKeys.DIFF_COLOR);
        }
        if (targets == null || targets.contains(AppSettingKeys.WORK_DIR_BASE)) {
            builder.setDefaultValue(AppSettingKeys.WORK_DIR_BASE);
        }
        if (targets == null || targets.contains(AppSettingKeys.CURR_TIMESTAMP)) {
            builder.setDefaultValue(AppSettingKeys.CURR_TIMESTAMP);
        }
        return builder.build();
    }
    
    /**
     * 比較処理を実行します。<br>
     * 
     * @throws IllegalStateException 必要な設定がなされておらず実行できない場合
     */
    public void execute() {
        if (!isReady.getValue()) {
            throw new IllegalStateException("I'm not ready.");
        }
        
        Settings settings = gatherSettings(null);
        AppMenu menu = settings.get(AppSettingKeys.CURR_MENU);
        
        if (!menu.isValidTargets(settings)) {
            new Alert(
                    AlertType.WARNING,
                    "同じブック同士／シート同士を比較することはできません。",
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        isRunning.set(true);
        
        Task<Void> task = AppTask.of(settings, Factory.basicFactoryOf());
        progressReport.progressProperty().bind(task.progressProperty());
        textReport.textProperty().bind(task.messageProperty());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            executor.shutdown();
            progressReport.progressProperty().unbind();
            progressReport.setProgress(0D);
            textReport.textProperty().unbind();
            if (settings.get(AppSettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            executor.shutdown();
            progressReport.progressProperty().unbind();
            progressReport.setProgress(0D);
            textReport.textProperty().unbind();
            new Alert(
                    AlertType.WARNING,
                    task.getException().getMessage(),
                    ButtonType.OK)
                            .showAndWait();
            isRunning.set(false);
        });
    }
}
