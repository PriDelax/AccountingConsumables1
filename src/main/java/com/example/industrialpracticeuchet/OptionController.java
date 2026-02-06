package com.example.industrialpracticeuchet;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public abstract class OptionController {
    protected Stage dialogStage;
    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public void closeDialog() { if (dialogStage != null) dialogStage.close(); }
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();}
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}