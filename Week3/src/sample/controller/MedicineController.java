package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import sample.model.Medicine;
import sample.service.MedicineService;
import sample.service.SettingsService;
import sample.view.AddMedicineView;
import sample.view.EditMedicineView;
import sample.view.MedicineView;
import sample.view.SettingsView;
import sample.view.MedicineListView;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MedicineController {

    private final MedicineView view;
    private final MedicineService service;
    private final SettingsService settingsService;
    private final ObservableList<Medicine> allMedicines;
    private final String username;

    public MedicineController() {
        this("Admin");
    }

    public MedicineController(String username) {
        this.username = username;
        view = new MedicineView(username);
        service = new MedicineService();
        settingsService = new SettingsService();
        allMedicines = FXCollections.observableArrayList(
                service.loadMedicines()
        );

        view.getMedicineTable().setItems(allMedicines);
        loadSavedSettings();
        refreshStatistics();

        setupMouseEvents();
        setupKeyboardEvents();
        setupSearch();
        setupHomeButton();
        setupAddButton();
        setupEditButton();
        setupSalesButton();
        setupMedicinesButton();
        setupSettingsButton();
        setupLogoutButton();
    }

    private void loadSavedSettings() {
        view.updateHeaderInfo(
                settingsService.getPharmacyName(),
                settingsService.getDisplayName(username)
        );
        view.updateSubtitle(settingsService.getSubtitle());
        view.setLowStockLimit(settingsService.getLowStockLimit());
    }

    private void refreshStatistics() {
        int total = allMedicines.size();
        int available = 0;
        int outOfStock = 0;

        for (Medicine medicine : allMedicines) {
            if (medicine.getQuantity() == 0) {
                outOfStock++;
            } else {
                available++;
            }
        }

        view.updateStatistics(total, available, outOfStock);
    }

    // =========================================================
    // HOME BUTTON (LOGIN CONTROLLER)
    // =========================================================

    private void setupHomeButton() {
        if (view.getHomeButton() == null) {
            return;
        }

        view.getHomeButton().setOnAction(event -> {
            try {
                LoginController loginController = new LoginController();
                Object loginView = loginController.getClass().getMethod("getView").invoke(loginController);
                Object root = loginView.getClass().getMethod("getRoot").invoke(loginView);

                if (view.getRoot().getScene() != null && root instanceof javafx.scene.Parent) {
                    view.getRoot().getScene().setRoot((javafx.scene.Parent) root);
                }
            } catch (Exception e) {
                showAlert("Info", "LoginController is ready, please match getView()/getRoot() in LoginView.");
            }
        });
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {
        view.getSearchField()
                .textProperty()
                .addListener((observable, oldValue, newValue) ->
                        filterMedicines(newValue)
                );
    }

    private void filterMedicines(String searchText) {
        ObservableList<Medicine> filtered = FXCollections.observableArrayList();
        String search = searchText.toLowerCase().trim();

        for (Medicine medicine : allMedicines) {
            if (medicine.getName().toLowerCase().startsWith(search)) {
                filtered.add(medicine);
            }
        }

        view.getMedicineTable().setItems(filtered);
    }

    // =========================================================
    // ADD MEDICINE
    // =========================================================

    private void setupAddButton() {
        view.getAddButton().setOnAction(event -> addMedicine());
    }

    private void addMedicine() {
        AddMedicineView addView = new AddMedicineView();

        addView.getSaveButton().setOnAction(event -> saveNewMedicine(addView));
        addView.getCancelButton().setOnAction(event -> addView.getStage().close());

        addView.show();
    }

    private void saveNewMedicine(AddMedicineView addView) {
        try {
            String name = addView.getNameField().getText().trim();
            String priceText = addView.getPriceField().getText().trim();
            String quantityText = addView.getQuantityField().getText().trim();

            if (name.isEmpty()) {
                showAlert("Invalid Data", "Medicine name cannot be empty.");
                return;
            }

            double price = Double.parseDouble(priceText);
            if (price < 0) {
                showAlert("Invalid Data", "Price cannot be negative.");
                return;
            }

            int quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                showAlert("Invalid Data", "Quantity cannot be negative.");
                return;
            }

            Medicine medicine = new Medicine(name, price, quantity);

            runInBackground(
                    addView.getSaveButton(),
                    () -> addMedicineSafely(service, medicine),
                    error -> finishSave(addView.getStage(), error, "Medicine added successfully.")
            );

        } catch (NumberFormatException e) {
            showAlert("Invalid Data", "Price must be a number and quantity must be an integer.");
        }
    }

    // =========================================================
    // EDIT MEDICINE
    // =========================================================

    private void setupEditButton() {
        view.getEditButton().setOnAction(event -> editSelectedMedicine());
    }

    private void editSelectedMedicine() {
        Medicine selectedMedicine = view.getMedicineTable()
                .getSelectionModel()
                .getSelectedItem();

        if (selectedMedicine == null) {
            showAlert("No Medicine Selected", "Please select a medicine first.");
            return;
        }

        EditMedicineView editView = new EditMedicineView(selectedMedicine);

        editView.getSaveButton().setOnAction(event ->
                saveEditedMedicine(selectedMedicine, editView)
        );
        editView.getCancelButton().setOnAction(event -> editView.getStage().close());

        editView.show();
    }

    private void saveEditedMedicine(Medicine oldMedicine, EditMedicineView editView) {
        try {
            String name = editView.getNameField().getText().trim();
            double price = Double.parseDouble(editView.getPriceField().getText().trim());
            int quantity = Integer.parseInt(editView.getQuantityField().getText().trim());

            if (name.isEmpty()) {
                showAlert("Invalid Data", "Medicine name cannot be empty.");
                return;
            }

            if (price < 0) {
                showAlert("Invalid Data", "Price cannot be negative.");
                return;
            }

            if (quantity < 0) {
                showAlert("Invalid Data", "Quantity cannot be negative.");
                return;
            }

            Medicine updatedMedicine = new Medicine(name, price, quantity);

            runInBackground(
                    editView.getSaveButton(),
                    () -> updateMedicineSafely(service, oldMedicine, updatedMedicine),
                    error -> finishSave(editView.getStage(), error, "Medicine updated successfully.")
            );

        } catch (NumberFormatException e) {
            showAlert("Invalid Data", "Price must be a number and quantity must be an integer.");
        }
    }

    // called on the JavaFX thread when a save finished (error == null -> success)

    private void finishSave(Stage stage, String error, String successMessage) {

        // always show the real data: the file may have been changed by someone else

        refreshMedicines();

        stage.close();

        if (error == null) {
            showAlert("Success", successMessage);
        } else {
            showAlert(Alert.AlertType.WARNING, "Not Applied", error);
        }
    }

    private void refreshMedicines() {
        allMedicines.setAll(service.loadMedicines());
        filterMedicines(view.getSearchField().getText());
        view.getMedicineTable().refresh();
        refreshStatistics();
    }

    // =========================================================
    // SAFE (SYNCHRONIZED) FILE OPERATIONS
    // public + static: SalesController uses the same ones, and every
    // window locks on the same object (MedicineService.class), so a sale,
    // an add and an edit can never read/write medicines.txt together.
    // =========================================================

    /**
     * Runs the work on a background thread, keeps the button disabled meanwhile,
     * then calls onDone ON THE JAVAFX THREAD with null (success) or an error message.
     */
    public static void runInBackground(
            Button button,
            Supplier<String> work,
            Consumer<String> onDone) {

        Task<String> task = new Task<String>() {

            @Override
            protected String call() {
                return work.get();
            }
        };

        button.setDisable(true);

        task.setOnSucceeded(event -> {
            button.setDisable(false);
            onDone.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            button.setDisable(false);

            Throwable error = task.getException();

            if (error != null) {
                error.printStackTrace();
            }

            onDone.accept("An unexpected error occurred while saving.");
        });

        Thread thread = new Thread(task);

        thread.setDaemon(true);

        thread.start();
    }

    // returns null when added, otherwise the reason it was NOT added

    public static String addMedicineSafely(MedicineService service, Medicine medicine) {

        synchronized (MedicineService.class) {
            return addIfNew(service, medicine);
        }
    }

    private static String addIfNew(MedicineService service, Medicine medicine) {

        for (Medicine existing : service.loadMedicines()) {

            if (existing.getName().equalsIgnoreCase(medicine.getName())) {
                return "A medicine named \"" + existing.getName() + "\" already exists. "
                        + "Your medicine was not added.";
            }
        }

        service.addMedicine(medicine);

        return null;
    }

    // returns null when updated, otherwise the reason the edit was NOT applied

    public static String updateMedicineSafely(
            MedicineService service,
            Medicine oldMedicine,
            Medicine updatedMedicine) {

        synchronized (MedicineService.class) {
            return updateIfUnchanged(service, oldMedicine, updatedMedicine);
        }
    }

    // the check and the update must happen together, otherwise
    // two users can both pass the check and one edit overwrites the other

    private static String updateIfUnchanged(
            MedicineService service,
            Medicine oldMedicine,
            Medicine updatedMedicine) {

        List<Medicine> current = service.loadMedicines();

        Medicine found = null;

        for (Medicine medicine : current) {

            if (medicine.getName().equalsIgnoreCase(oldMedicine.getName())) {
                found = medicine;
                break;
            }
        }

        if (found == null) {
            return "\"" + oldMedicine.getName() + "\" no longer exists "
                    + "(someone else renamed or removed it). Your edit was not applied.";
        }

        boolean changedByOthers =
                Double.compare(found.getPrice(), oldMedicine.getPrice()) != 0
                        || found.getQuantity() != oldMedicine.getQuantity();

        if (changedByOthers) {
            return "\"" + found.getName() + "\" was changed by someone else "
                    + "(now: price " + found.getPrice() + ", stock " + found.getQuantity() + "). "
                    + "Your edit was not applied. The table now shows the latest data, "
                    + "please review it and edit again.";
        }

        boolean renamed = !updatedMedicine.getName().equalsIgnoreCase(oldMedicine.getName());

        if (renamed) {

            for (Medicine medicine : current) {

                if (medicine.getName().equalsIgnoreCase(updatedMedicine.getName())) {
                    return "A medicine named \"" + medicine.getName() + "\" already exists. "
                            + "Your edit was not applied.";
                }
            }
        }

        service.updateMedicine(oldMedicine, updatedMedicine);

        return null;
    }

    // =========================================================
    // ALERT
    // =========================================================

    private void showAlert(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================
    // MOUSE EVENTS
    // =========================================================

    private void setupMouseEvents() {
        view.getSearchField().setOnMousePressed(event ->
                System.out.println("Mouse Pressed")
        );

        view.getSearchField().setOnMouseReleased(event ->
                System.out.println("Mouse Released")
        );

        view.getSearchField().setOnMouseEntered(event ->
                System.out.println("Mouse Entered Search Field")
        );

        view.getSearchField().setOnMouseExited(event ->
                System.out.println("Mouse Exited Search Field")
        );

        view.getSearchField().setOnMouseMoved(event ->
                System.out.println("Mouse Moved")
        );

        view.getSearchField().setOnMouseDragged(event ->
                System.out.println("Mouse Dragged")
        );

        view.getSearchField().setOnMouseClicked(event -> {
            System.out.println("Mouse Clicked");
            if (event.getClickCount() == 2) {
                view.getSearchField().clear();
            }
        });
    }

    // =========================================================
    // KEYBOARD EVENTS
    // =========================================================

    private void setupKeyboardEvents() {
        view.getSearchField().setOnKeyPressed(event -> {
            System.out.println("Key Pressed: " + event.getCode());
            if (event.getCode() == KeyCode.ESCAPE) {
                view.getSearchField().clear();
            }
        });

        view.getSearchField().setOnKeyReleased(event ->
                System.out.println("Key Released: " + event.getCode())
        );

        view.getSearchField().setOnKeyTyped(event ->
                System.out.println("Key Typed: " + event.getCharacter())
        );
    }

    // =========================================================
    // SALES BUTTON
    // =========================================================

    private void setupSalesButton() {
        view.getSalesButton().setOnAction(event ->
                showAlert("Info", "Sales screen is not connected yet.")
        );
    }

    // =========================================================
    // SETTINGS BUTTON
    // =========================================================

    private void setupSettingsButton() {
        view.getSettingsButton().setOnAction(event -> openSettings());
    }

    private void openSettings() {

        // another window may have saved new values since we started

        settingsService.reload();

        SettingsView settingsView = new SettingsView();

        settingsView.getPharmacyNameField().setText(settingsService.getPharmacyName());
        settingsView.getAddressField().setText(settingsService.getAddress());
        settingsView.getPhoneField().setText(settingsService.getPhone());
        settingsView.getUserNameField().setText(settingsService.getUserName());
        settingsView.getCurrencyField().setText(settingsService.getCurrency());
        settingsView.getLowStockField().setText(String.valueOf(settingsService.getLowStockLimit()));
        settingsView.getConfirmSaleCheck().setSelected(settingsService.isConfirmSale());

        settingsView.getSaveButton().setOnAction(e -> saveSettings(settingsView));
        settingsView.getCancelButton().setOnAction(e -> settingsView.getStage().close());

        settingsView.show();
    }

    private void saveSettings(SettingsView settingsView) {

        boolean passwordChanged =
                !settingsView.getNewPasswordField().getText().trim().isEmpty();

        // validation + saving + password change all happen in SettingsService

        String error = settingsService.saveSettings(
                username,
                settingsView.getPharmacyNameField().getText(),
                settingsView.getAddressField().getText(),
                settingsView.getPhoneField().getText(),
                settingsView.getUserNameField().getText(),
                settingsView.getCurrencyField().getText(),
                settingsView.getLowStockField().getText(),
                settingsView.getConfirmSaleCheck().isSelected(),
                settingsView.getCurrentPasswordField().getText(),
                settingsView.getNewPasswordField().getText(),
                settingsView.getConfirmPasswordField().getText()
        );

        if (error != null) {
            showAlert("Invalid Data", error);
            return;
        }

        loadSavedSettings();

        settingsView.getStage().close();

        showAlert("Success", passwordChanged
                ? "Settings saved and password changed."
                : "Settings saved successfully.");
    }
    private void setupMedicinesButton() {
        view.getMedicinesButton().setOnAction(event -> {
            showAllMedicines();
        });
    }

    private void showAllMedicines() {
        MedicineListView listView =
                new MedicineListView(service.loadMedicines());
        listView.show();
    }

    // =========================================================
    // LOGOUT BUTTON
    // =========================================================

    private void setupLogoutButton() {
        view.getLogoutButton().setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Logout");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to logout?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    view.getRoot().getScene().getWindow().hide();
                }

            });
        });
    }

    // =========================================================
    // GET VIEW
    // =========================================================

    public MedicineView getView() {
        return view;
    }
}