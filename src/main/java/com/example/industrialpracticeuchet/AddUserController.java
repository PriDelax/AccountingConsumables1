package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class AddUserController extends OptionController {
    @FXML private TextField nameField;
    @FXML private CheckBox adminCheckBox;
    @FXML
    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Введите имя пользователя");
            return;
        }
        try {
            UserDAO dao = new UserDAO();
            dao.createUser(name, adminCheckBox.isSelected());
            closeDialog();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                showError("Пользователь с таким именем уже существует");
            } else {
                showError("Ошибка: " + e.getMessage());
            }
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }
}
