package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import java.util.List;

public class StockInController extends OptionController{
    @FXML private ComboBox<EquipmentDAO.Equipment> materialComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private TextField documentField;
    private String performedBy;

    public void setCurrentUser(String name) {
        this.performedBy = name;
    }
    @FXML
    public void initialize() {
        try {
            EquipmentDAO dao = new EquipmentDAO();
            List<EquipmentDAO.Equipment> materials = dao.findAll();
            materialComboBox.getItems().addAll(materials);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не удалось загрузить список материалов");
        }
    }
    @FXML
    private void accept() {
        EquipmentDAO.Equipment selected = materialComboBox.getValue();
        if (selected == null) {
            showError("Выберите материал");
            return;
        }
        int quantity = quantitySpinner.getValue();
        if (quantity <= 0) {
            showError("Количество должно быть больше 0");
            return;
        }
        String documentRef = documentField.getText().trim();
        try {
            StockOperationDAO dao = new StockOperationDAO();
            dao.createIn(selected.id, quantity, documentRef, performedBy);
            closeDialog();
        } catch (Exception e) {
            showError("Ошибка поступления: " + e.getMessage());
        }
    }
    @FXML
    private void cancel() {
        closeDialog();
    }
}
