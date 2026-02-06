package com.example.industrialpracticeuchet;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController extends OptionController {
    @FXML private Button addMaterialButton, stockInButton, stockOutButton, purchaseButton, editMaterialButton, deleteMaterialButton, createRequestButton, fulfillRequestButton, exportHistoryButton, cancelRequestButton, refreshDataButton, exportMyHistoryButton;
    @FXML private Label userLabel;
    @FXML private TabPane tabPane;
    @FXML private Menu adminMenu;
    @FXML private TextField searchField;
    @FXML private TableView<EquipmentDAO.Equipment> materialsTable;
    @FXML private TableColumn<EquipmentDAO.Equipment, String> nameColumn, categoryColumn, articleColumn;
    @FXML private TableColumn<EquipmentDAO.Equipment, Integer> stockColumn;
    @FXML private TableView<RequestDAO.Request> requestsTable;
    @FXML private TableColumn<RequestDAO.Request, String> reqMaterialColumn, reqFromColumn, reqStatusColumn;
    @FXML private TableColumn<RequestDAO.Request, Integer> reqQtyColumn;
    @FXML private TableView<StockOperationDAO.Operation> operationsTable;
    @FXML private TableColumn<StockOperationDAO.Operation, String> opDateColumn, opMaterialColumn, opTypeColumn, opDestColumn, opReasonColumn, opUserColumn;
    @FXML private TableColumn<StockOperationDAO.Operation, Integer> opQtyColumn, opBalanceColumn;
    @FXML private TableColumn<EquipmentDAO.Equipment, Void> imageColumn;
    @FXML private ComboBox<String> operationTypeFilter;
    private UserDAO.User currentUser;
    private static final DateTimeFormatter EXCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @FXML
    private void refreshData() {
        loadMaterials();
        loadRequests();
        loadOperationsHistory();
        showAlert("Успех", "Данные обновлены");
    }

    public void setCurrentUser(UserDAO.User user) {
        this.currentUser = user;
        userLabel.setText("Вы вошли как: " + user.name);
        updateUIByRole();
        loadMaterials();
        loadRequests();
        loadOperationsHistory();
    }

    private void updateUIByRole() {
        boolean isAdmin = currentUser.isAdmin();
        adminMenu.setVisible(isAdmin);
        fulfillRequestButton.setVisible(isAdmin);
        addMaterialButton.setVisible(isAdmin);
        stockInButton.setVisible(isAdmin);
        stockOutButton.setVisible(isAdmin);
        purchaseButton.setVisible(isAdmin);
        editMaterialButton.setVisible(isAdmin);
        deleteMaterialButton.setVisible(isAdmin);
    }

    @FXML
    private void stockIn() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("stock_in.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Поступление на склад");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            StockInController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setCurrentUser(currentUser.name);
            stage.showAndWait();
            loadOperationsHistory();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму поступления");
        }
    }

    @FXML
    private void addMaterial() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_material.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Добавить материал");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            AddMaterialController controller = loader.getController();
            controller.setDialogStage(stage);
            stage.showAndWait();
            loadMaterials();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму");
        }
    }

    private void loadMaterials() {
        try {
            EquipmentDAO dao = new EquipmentDAO();
            String query = searchField.getText().trim();
            List<EquipmentDAO.Equipment> list;
            if (query.isEmpty()){
                list = dao.findAll();
            } else {
                list = dao.search(query);
            }
            materialsTable.getItems().setAll(list);
        } catch (Exception e) {
            showError("Ошибка загрузки материалов");
        }
    }

    private void loadRequests() {
        try {
            RequestDAO dao = new RequestDAO();
            List<RequestDAO.Request> pending = dao.findByStatus(RequestDAO.Status.ОЖИДАЕТСЯ);
            requestsTable.getItems().setAll(pending);
        } catch (Exception e) {
            showError("Ошибка загрузки заявок");
        }
    }

    @FXML
    private void exportHistory() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        if (!currentUser.isAdmin()) return;
        loadOperationsHistory();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить историю операций");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
        chooser.setInitialFileName("История_операций_" + java.time.LocalDate.now() + ".csv");
        File file = chooser.showSaveDialog(userLabel.getScene().getWindow());
        if (file == null) return;
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)){
        writer.write('\uFEFF');
        writer.write("Дата;Материал;Тип;Кол-во;Остаток после;От/Кому;Причина;Ответственный\n");
        for (StockOperationDAO.Operation op : operationsTable.getItems()) {
            String formattedDate = formatForExcel(op.createdAt);
            String type = "ПОСТУПЛЕНИЕ".equals(op.operationType) ? "Поступление" : "Списание";
            String line = String.format(
                    "\"%s\";\"%s\";\"%s\";%d;%d;\"%s\";\"%s\";\"%s\"",
                    formattedDate,
                    op.materialName,
                    type,
                    op.quantity,
                    op.balanceAfter,
                    op.destination != null ? op.destination : "",
                    op.reason != null ? op.reason : "",
                    op.performedBy
            );
            writer.write(line + "\n");
        }
            showAlert("Успех", "История экспортирована.\nЕсли В Excel видны #####,дважды щелкните по краю столбца с буквой.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка экспорта: " + e.getMessage());
        }
    }

    @FXML
    private void fulfillRequest() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        RequestDAO.Request selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите заявку для выдачи");
            return;
        }
        try {
            RequestDAO dao = new RequestDAO();
            dao.fulfill(selected.id, currentUser.name);
            loadRequests();
            loadMaterials();
            showAlert("Успех", "Материал выдан");
        } catch (Exception e) {
            showError("Ошибка выдачи: " + e.getMessage());
        }
    }

    @FXML
    private void manageUsers() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        showAddUserDialog();
    }

    private void loadOperationsHistory() {
        try {
            StockOperationDAO dao = new StockOperationDAO();
            List<StockOperationDAO.Operation> ops;
            if (currentUser.isAdmin()) {
                String selected = operationTypeFilter.getValue();
                String filterType = null;
                if ("Поступление".equals(selected)) filterType = "ПОСТУПЛЕНИЕ";
                else if ("Списание".equals(selected)) filterType = "СПИСАНИЕ";
                ops = dao.findAll(filterType);
            } else {
                ops = dao.findByUser(currentUser.name);
            }
            operationsTable.getItems().setAll(ops);
        } catch (Exception e) {
            showError("Ошибка загрузки истории: " + e.getMessage());
        }
    }
    @FXML
    private void exportMyHistory() {
        try {
            StockOperationDAO dao = new StockOperationDAO();
            List<StockOperationDAO.Operation> myOps = dao.findByUser(currentUser.name);
            if (myOps.isEmpty()) {
                showAlert("Информация", "У вас нет операций для экспорта.");
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Сохранить мою историю операций");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
            chooser.setInitialFileName("Моя_история_" + java.time.LocalDate.now() + ".csv");
            File file = chooser.showSaveDialog(userLabel.getScene().getWindow());
            if (file == null) return;
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                writer.write("Дата;Материал;Тип;Кол-во;Остаток после;От/Кому;Причина;Ответственный\n");
                for (StockOperationDAO.Operation op : myOps) {
                    String formattedDate = formatForExcel(op.createdAt);
                    String type = "ПОСТУПЛЕНИЕ".equals(op.operationType) ? "Поступление" : "Списание";
                    String line = String.format(
                            "\"%s\";\"%s\";\"%s\";%d;%d;\"%s\";\"%s\";\"%s\"",
                            formattedDate,
                            op.materialName != null ? op.materialName : "",
                            type,
                            op.quantity,
                            op.balanceAfter,
                            op.destination != null ? op.destination : "",
                            op.reason != null ? op.reason : "",
                            op.performedBy != null ? op.performedBy : ""
                    );
                    writer.write(line + "\n");
                }
                showAlert("Успех", "Личная история экспортирована:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка экспорта: " + e.getMessage());
        }
    }
    @FXML
    private void logout() {
        SessionManager.clearSession();
        Stage stage = (Stage) userLabel.getScene().getWindow();
        stage.close();
        Main.showLoginWindow();
    }
    @FXML
    private void createRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("create_request.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Создать заявку");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            CreateRequestController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setCurrentUser(currentUser);
            stage.showAndWait();
            loadRequests();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму заявки");
        }
    }

    @FXML
    private void stockOut() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("stock_out.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Списание");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            StockOutController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setCurrentUser(currentUser.name);
            stage.showAndWait();
            loadOperationsHistory();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму списания");
        }
    }

    @FXML
    private void cancelMyRequest() {
        RequestDAO.Request selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите заявку для отмены");
            return;
        }
        if (!selected.requester.equals(currentUser.name)) {
            showError("Нельзя отменить чужую заявку");
            return;
        }
        if (!"ОЖИДАЕТСЯ".equals(selected.status.name())) {
            showError("Заявка уже обработана");
            return;
        }
        try {
            RequestDAO dao = new RequestDAO();
            dao.cancel(selected.id, currentUser.name);
            loadRequests();
            showAlert("Успех", "Заявка отменена");
        } catch (Exception e) {
            showError("Ошибка отмены: " + e.getMessage());
        }
    }

    @FXML
    private void editMaterial() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        EquipmentDAO.Equipment selected = materialsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Внимание", "Выберите материал для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("edit-material.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Редактировать материал");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            EditMaterialController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setMaterial(selected);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму редактирования");
        }
    }

    @FXML
    private void deleteMaterial() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        EquipmentDAO.Equipment selected = materialsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Внимание", "Выберите материал для удаления");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите удалить материал:\n" + selected.name + "?\n\n" +
                "Удаление невозможно, если по материалу есть операции.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    EquipmentDAO dao = new EquipmentDAO();
                    boolean success = dao.delete(selected.id);
                    if (success) {
                        loadMaterials();
                        loadOperationsHistory();
                        showAlert("Успех", "Материал удалён");
                    } else {
                        showError("Невозможно удалить: по материалу есть операции");
                    }
                } catch (Exception e) {
                    showError("Ошибка удаления: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void exportPurchaseRequest() {
        if (!currentUser.isAdmin()) {
            showError("Доступ запрещён");
            return;
        }
        if (!currentUser.isAdmin()) return;
        try {
            EquipmentDAO dao = new EquipmentDAO();
            List<EquipmentDAO.PurchaseItem> items = dao.getItemsForPurchase();
            if (items.isEmpty()) {
                showAlert("Информация", "Все материалы в наличии. Закупка не требуется.");
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Сохранить заявку на закупку");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
            chooser.setInitialFileName("Заявка_на_закупку_" + java.time.LocalDate.now() + ".csv");
            File file = chooser.showSaveDialog(userLabel.getScene().getWindow());
            if (file == null) return;
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                writer.write("Наименование;Артикул;Текущий остаток;Рекомендуемое количество\n");
                for (EquipmentDAO.PurchaseItem item : items) {
                    String line = String.format(
                            "\"%s\";\"%s\";%d;%d",
                            item.name != null ? item.name : "",
                            item.article != null ? item.article : "",
                            item.currentStock,
                            item.recommendedQty
                    );
                    writer.write(line + "\n");
                }
                showAlert("Успех", "Заявка на закупку сохранена:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Ошибка экспорта: " + e.getMessage());
        }
    }

    @FXML
    private void exit() {
        System.exit(0);
    }

    private void showAddUserDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_user.fxml"));
            Region root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Добавить пользователя");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(userLabel.getScene().getWindow());
            AddUserController controller = loader.getController();
            controller.setDialogStage(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не удалось открыть форму");
        }
    }

    @FXML
    private void changeUserName() {
        TextInputDialog dialog = new TextInputDialog(currentUser.name);
        dialog.setTitle("Изменить имя");
        dialog.setHeaderText("Введите новое имя:");
        dialog.setContentText("Имя пользователя:");
        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty()) {
                showError("Имя не может быть пустым");
                return;
            }
            try {
                UserDAO dao = new UserDAO();
                dao.updateName(currentUser.name, newName);
                currentUser.name = newName;
                userLabel.setText("Вы вошли как: " + newName);
                showAlert("Успех", "Имя изменено");
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });
    }

    public void initialize() {
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name));
        categoryColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName));
        articleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().article));
        stockColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().currentStock).asObject());
        imageColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                setGraphic(imageView);
                setPrefHeight(70);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                EquipmentDAO.Equipment equipment = getTableRow() != null ? getTableRow().getItem() : null;
                if (equipment != null && equipment.image_path != null && !equipment.image_path.isEmpty()) {
                    File imgFile = new File(equipment.image_path);
                    if (imgFile.exists()) {
                        imageView.setImage(new Image(imgFile.toURI().toString()));
                        imageView.setVisible(true);
                    } else {
                        imageView.setVisible(false);
                    }
                } else {
                    imageView.setVisible(false);
                }
            }
        });
        reqMaterialColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().equipmentName));
        reqQtyColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().quantity).asObject());
        reqFromColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().requester));
        reqStatusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status.name()));
        opDateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatForExcel(data.getValue().createdAt)));
        opMaterialColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().materialName));
        opTypeColumn.setCellValueFactory(data -> new SimpleStringProperty("ПОСТУПЛЕНИЕ".equals(data.getValue().operationType) ? "Поступление" : "Списание"));
        opQtyColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().quantity).asObject());
        opBalanceColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().balanceAfter).asObject());
        opDestColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().destination));
        opReasonColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reason));
        opUserColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().performedBy));
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        operationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        operationTypeFilter.getItems().addAll("Все", "Поступление", "Списание");
        operationTypeFilter.setValue("Все");
        operationTypeFilter.setOnAction(e -> loadOperationsHistory());
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            loadMaterials();
        });
        Platform.runLater(() -> {
            hideAllActionButtons();
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {
                String title = selectedTab.getText();
                if ("Материалы на складе".equals(title)) {
                    showMaterialButtons();
                } else if ("Заявки".equals(title)) {
                    showRequestButtons();
                } else if ("История операций".equals(title)) {
                    showHistoryButtons();
                }
            } else {
                tabPane.getSelectionModel().select(0);
                showMaterialButtons();
            }
        });
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            hideAllActionButtons();
            if (newTab == null) return;
            String title = newTab.getText();
            if ("Материалы на складе".equals(title)) {
                showMaterialButtons();
            } else if ("Заявки".equals(title)) {
                showRequestButtons();
            } else if ("История операций".equals(title)) {
                showHistoryButtons();
            }
        });
    }

    private void hideAllActionButtons() {
        createRequestButton.setVisible(false);
        fulfillRequestButton.setVisible(false);
        addMaterialButton.setVisible(false);
        stockInButton.setVisible(false);
        stockOutButton.setVisible(false);
        purchaseButton.setVisible(false);
        editMaterialButton.setVisible(false);
        deleteMaterialButton.setVisible(false);
        cancelRequestButton.setVisible(false);
        exportHistoryButton.setVisible(false);
        refreshDataButton.setVisible(false);
        exportMyHistoryButton.setVisible(false);
    }

    private void showMaterialButtons() {
        addMaterialButton.setVisible(true);
        editMaterialButton.setVisible(true);
        deleteMaterialButton.setVisible(true);
        stockInButton.setVisible(true);
        stockOutButton.setVisible(true);
        refreshDataButton.setVisible(true);
    }

    private void showRequestButtons() {
        createRequestButton.setVisible(true);
        fulfillRequestButton.setVisible(true);
        cancelRequestButton.setVisible(true);
        refreshDataButton.setVisible(true);
    }

    private void showHistoryButtons() {
        exportHistoryButton.setVisible(true);
        purchaseButton.setVisible(true);
        refreshDataButton.setVisible(true);
        exportMyHistoryButton.setVisible(true);
    }

    private String formatForExcel(String dbDateTime) {
        if (dbDateTime == null || dbDateTime.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(dbDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (Exception e) {
            return dbDateTime;
        }
    }
}
