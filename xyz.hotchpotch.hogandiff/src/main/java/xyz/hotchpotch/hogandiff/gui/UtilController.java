package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

public class UtilController extends HBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private Button buttonShowWorkDir;
    
    @FXML
    private Button buttonDeleteOldWorkDir;
    
    @FXML
    private Hyperlink linkToWebSite;
    
    public UtilController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UtilView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public void init(Path workDir) {
        Objects.requireNonNull(workDir, "workDir");
        
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
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    "次のフォルダの内容物を全て削除します。よろしいですか？\n" + workDir)
                            .showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Stream<Path> children = Files.walk(workDir)) {
                    children.filter(path -> !path.equals(workDir))
                            .sorted(Comparator.reverseOrder())
                            .forEach(UnsafeConsumer.toConsumer(Files::deleteIfExists));
                } catch (Exception e) {
                    //nop
                }
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
}