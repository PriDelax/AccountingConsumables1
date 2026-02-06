package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.util.List;

public class StockOutController extends OptionController{
    @FXML private ComboBox<EquipmentDAO.Equipment> materialComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private TextField destinationField;
    @FXML private TextArea reasonArea;
    private String performedBy;

    public void setCurrentUser(String name) {
        this.performedBy = name;
    }
    @FXML
    public void initialize() {
        try {
            EquipmentDAO dao = new EquipmentDAO();
            List<EquipmentDAO.Equipment> materials = dao.findAll();
            materials.removeIf(m -> m.currentStock <= 0);
            materialComboBox.getItems().addAll(materials);
        } catch (Exception e) {
            showError("Ошибка загрузки материалов");
        }
    }
    @FXML
    private void writeOff() {
        EquipmentDAO.Equipment selected = materialComboBox.getValue();
        if (selected == null) {
            showError("Выберите материал");
            return;
        }
        int quantity = quantitySpinner.getValue();
        if (quantity <= 0) {
            showError("Количество должно быть > 0");
            return;
        }
        if (quantity > selected.currentStock) {
            showError("На складе только " + selected.currentStock + " шт.");
            return;
        }
        String destination = destinationField.getText().trim();
        String reason = reasonArea.getText().trim();
        if (reason.isEmpty()) {
            showError("Укажите причину списания");
            return;
        }
        try {
            StockOperationDAO dao = new StockOperationDAO();
            dao.createOut(selected.id, quantity, destination, reason, performedBy, null);
            closeDialog();
        } catch (Exception e) {
            showError("Ошибка списания: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }
}
