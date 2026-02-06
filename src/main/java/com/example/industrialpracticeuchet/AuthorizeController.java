package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AuthorizeController extends OptionController implements Initializable {
    @FXML private ComboBox<UserDAO.User> userComboBox;
    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            UserDAO userDAO = new UserDAO();
            List<UserDAO.User> users = userDAO.findAll();
            userComboBox.getItems().addAll(users);
            String savedName = SessionManager.getCurrentUser();
            if (savedName != null) {
                for (UserDAO.User u : users) {
                    if (u.name.equals(savedName)) {
                        userComboBox.setValue(u);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке пользователей");
            e.printStackTrace();
        }
    }

    @FXML
    private void login() {
        UserDAO.User selected = userComboBox.getValue();
        if (selected == null) {
            showError("Пожалуйста, выберите пользователя");
            return;
        }
        SessionManager.setCurrentUser(selected.name);
        if (dialogStage != null) {
            dialogStage.close();
        }
        Main.showMainWindow(selected);
    }
}
