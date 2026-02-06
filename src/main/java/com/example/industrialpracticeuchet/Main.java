package com.example.industrialpracticeuchet;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    private static UserDAO.User currentUser;

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) {
        String savedUser = SessionManager.getCurrentUser();
        if (savedUser != null) {
            try {
                UserDAO.User user = new UserDAO().findByName(savedUser);
                if (user != null) {
                    currentUser = user;
                    showMainWindow(user);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            SessionManager.clearSession();
        }
        showLoginWindow();
    }

    public static void showLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("authorization.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Вход");
            stage.setScene(scene);
            stage.setResizable(false);
            AuthorizeController controller = loader.getController();
            controller.setDialogStage(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showMainWindow(UserDAO.User user) {
        currentUser = user;
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Склад.Расходники — " + user.name);
            stage.setScene(scene);
            stage.show();
            Object controller = loader.getController();
            if (controller instanceof MainController) {
                ((MainController) controller).setCurrentUser(user);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
