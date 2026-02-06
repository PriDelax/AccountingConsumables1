package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AddMaterialController extends OptionController {
    @FXML private TextField nameField, articleField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Spinner<Integer> minStockSpinner;
    @FXML private Label imagePathLabel;
    @FXML private ImageView previewImage;
    private String selectedImagePath;

    @FXML
    void initialize() {
        try {
            CategoryDAO dao = new CategoryDAO();
            List<String> categories = dao.findAllNames();
            categoryComboBox.getItems().addAll(categories);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void addCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая категория");
        dialog.setHeaderText("Введите название категории");
        dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                categoryComboBox.getItems().add(name.trim());
                categoryComboBox.setValue(name.trim());
            }
        });
    }

    @FXML
    void selectImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            imagePathLabel.setText(file.getName());
            previewImage.setVisible(true);
            previewImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    void save() {
        String name = nameField.getText().trim();
        String category = categoryComboBox.getValue();
        if (name.isEmpty() || category == null) {
            showError("Заполните обязательные поля");
            return;
        }
        String article = articleField.getText().trim();
        int minStock = minStockSpinner.getValue();
        String savedImagePath = null;
        if (selectedImagePath != null) {
            File sourceFile = new File(selectedImagePath);
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                showError("Файл изображения не найден или недоступен");
                return;
            }
            try {
                File imagesDir = new File("images");
                if (!imagesDir.exists()) imagesDir.mkdirs();
                String fileName = System.currentTimeMillis() + "_" + new File(selectedImagePath).getName();
                File destFile = new File(imagesDir, fileName);
                Path sourcePath = Paths.get(selectedImagePath);
                Path destPath = destFile.toPath();
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                savedImagePath = destFile.getAbsolutePath();
            } catch (IOException e) {
                showError("Ошибка сохранения изображения");
                return;
            }
        }
        try {
            int initialStock = 0;
            EquipmentDAO dao = new EquipmentDAO();
            dao.create(name, category, article, minStock, initialStock);
            EquipmentDAO.Equipment eq = findLastAdded(name, article);
            if (eq != null && savedImagePath != null) {
                updateImagePath(eq.id, savedImagePath);
            }
            closeDialog();
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    EquipmentDAO.Equipment findLastAdded(String name, String article) throws SQLException {
        String sql = "SELECT id FROM equipment WHERE name = ? AND article = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, article != null ? article : "");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EquipmentDAO.Equipment eq = new EquipmentDAO.Equipment();
                    eq.id = rs.getInt("id");
                    return eq;
                }
            }
        }
        return null;
    }

    void updateImagePath(int id, String path) throws SQLException {
        String sql = "UPDATE equipment SET image_path = ? WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }
}
