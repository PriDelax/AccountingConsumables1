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
import java.util.List;

public class EditMaterialController extends OptionController{

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField articleField;
    @FXML private Spinner<Integer> minStockSpinner;
    @FXML private Label imagePathLabel;
    @FXML private ImageView previewImage;
    private EquipmentDAO.Equipment originalMaterial;
    private String selectedImagePath;

    public void setMaterial(EquipmentDAO.Equipment material) {
        this.originalMaterial = material;
        nameField.setText(material.name);
        articleField.setText(material.article);
        minStockSpinner.getValueFactory().setValue(material.minStock);
        try {
            CategoryDAO dao = new CategoryDAO();
            List<String> categories = dao.findAllNames();
            categoryComboBox.getItems().setAll(categories);
            categoryComboBox.setValue(material.categoryName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (material.image_path != null && !material.image_path.isEmpty()) {
            File imgFile = new File(material.image_path);
            if (imgFile.exists()) {
                previewImage.setImage(new Image(imgFile.toURI().toString()));
                previewImage.setVisible(true);
                imagePathLabel.setText(imgFile.getName());
                selectedImagePath = material.image_path;
            }
        }
    }
    @FXML
    private void addCategory() {
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
    private void selectImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            imagePathLabel.setText(file.getName());
            previewImage.setVisible(true);
            previewImage.setImage(new Image(file.toURI().toString()));
        }
    }
    @FXML
    private void removeImage() {
        selectedImagePath = null;
        previewImage.setVisible(false);
        imagePathLabel.setText("(не выбрано)");
    }
    @FXML
    private void save() {
        String name = nameField.getText().trim();
        String category = categoryComboBox.getValue();
        if (name.isEmpty() || category == null) {
            showError("Заполните обязательные поля");
            return;
        }
        String article = articleField.getText().trim();
        int minStock = minStockSpinner.getValue();
        String savedImagePath = selectedImagePath;
        if (selectedImagePath != null && !selectedImagePath.equals(originalMaterial.image_path)) {
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
            CategoryDAO catDao = new CategoryDAO();
            Integer categoryId = catDao.findIdByName(category);
            if (categoryId == null) {
                catDao.create(category);
                categoryId = catDao.findIdByName(category);
            }
            String sql = """
                UPDATE equipment
                SET name = ?, category_id = ?, article = ?, min_stock = ?, image_path = ?
                WHERE id = ?
                """;
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, categoryId);
                stmt.setString(3, article);
                stmt.setInt(4, minStock);
                stmt.setString(5, savedImagePath);
                stmt.setInt(6, originalMaterial.id);
                stmt.executeUpdate();
            }
            closeDialog();
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }
    @FXML
    private void cancel() {
        closeDialog();
    }
}
