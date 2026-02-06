package com.example.industrialpracticeuchet;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import java.util.List;

public class CreateRequestController extends OptionController {
    @FXML private ComboBox<EquipmentDAO.Equipment> materialComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private TextArea reasonArea;
    private UserDAO.User currentUser;

    public void setCurrentUser(UserDAO.User user) {
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        try {
            EquipmentDAO dao = new EquipmentDAO();
            List<EquipmentDAO.Equipment> materials = dao.findAll();
            materials.removeIf(m -> m.currentStock <= 0);
            materialComboBox.getItems().addAll(materials);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не удалось загрузить список материалов");
        }
    }

    @FXML
    private void create() {
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
        if (quantity > selected.currentStock) {
            showError("На складе доступно только " + selected.currentStock + " шт.");
            return;
        }
        String reason = reasonArea.getText().trim();
        try {
            RequestDAO requestDAO = new RequestDAO();
            requestDAO.create(selected.id, quantity, currentUser.name, reason);
            closeDialog();
        } catch (Exception e) {
            showError("Ошибка создания заявки: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }
}
