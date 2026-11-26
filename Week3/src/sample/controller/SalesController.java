package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import sample.model.Medicine;
import sample.model.Sale;
import sample.service.MedicineService;
import sample.service.SalesService;
import sample.service.SettingsService;
import sample.view.AddMedicineView;
import sample.view.EditMedicineView;
import sample.view.MedicineListView;
import sample.view.ReportsView;
import sample.view.SalesView;
import sample.view.SettingsView;

import java.util.ArrayList;
import java.util.List;

public class SalesController {

    private final String cashierName;

    private final SalesView view;
    private final MedicineService medicineService;
    private final SalesService salesService;
    private final SettingsService settingsService;

    private final ObservableList<Medicine> allMedicines;
    private final FilteredList<Medicine> filteredMedicines;
    private final ObservableList<Sale> cart;

    public SalesController(String username) {

        this.cashierName = username;

        view = new SalesView(username);
        medicineService = new MedicineService();
        salesService = new SalesService();
        settingsService = new SettingsService();

        allMedicines = FXCollections.observableArrayList(medicineService.loadMedicines());
        filteredMedicines = new FilteredList<>(allMedicines, m -> true);
        cart = FXCollections.observableArrayList();

        view.getMedicineTable().setItems(filteredMedicines);
        view.getCartTable().setItems(cart);

        loadSavedSettings();

        // ---- sales page ----

        setupSearch();
        setupAddToCart();
        setupRemove();
        setupClear();
        setupCheckout();

        // ---- sidebar (same buttons as MedicineController) ----

        setupHomeButton();
        setupMedicinesButton();
        setupSalesButton();
        setupReportsButton();
        setupAddButton();
        setupEditButton();
        setupSettingsButton();
        setupLogoutButton();
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        view.getSearchField().textProperty().addListener((obs, oldValue, newValue) -> {

            String search = newValue.toLowerCase().trim();

            filteredMedicines.setPredicate(
                    medicine -> medicine.getName().toLowerCase().startsWith(search)
            );
        });
    }

    // =========================================================
    // ADD TO CART
    // =========================================================

    private void setupAddToCart() {
        view.getAddToCartButton().setOnAction(event -> addToCart());
    }

    private void addToCart() {

        Medicine selected = view.getMedicineTable().getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Medicine Selected", "Please select a medicine first.");
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(view.getQuantityField().getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Quantity must be an integer.");
            return;
        }

        if (quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Quantity must be greater than zero.");
            return;
        }

        // same medicine already in cart?

        Sale existing = findInCart(selected.getName());

        int alreadyInCart = (existing == null) ? 0 : existing.getQuantity();

        if (alreadyInCart + quantity > selected.getQuantity()) {
            showAlert(Alert.AlertType.WARNING, "Not Enough Stock",
                    "Available: " + selected.getQuantity() + " | In cart: " + alreadyInCart);
            return;
        }

        if (existing == null) {
            cart.add(new Sale(selected.getName(), selected.getPrice(), quantity));
        } else {
            existing.setQuantity(alreadyInCart + quantity);
            view.getCartTable().refresh();
        }

        view.getQuantityField().setText("1");
        updateTotal();
    }

    private Sale findInCart(String name) {

        for (Sale sale : cart) {
            if (sale.getMedicineName().equals(name)) {
                return sale;
            }
        }

        return null;
    }

    // =========================================================
    // REMOVE / CLEAR
    // =========================================================

    private void setupRemove() {

        view.getRemoveButton().setOnAction(event -> {

            Sale selected = view.getCartTable().getSelectionModel().getSelectedItem();

            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Nothing Selected", "Select an item from the cart first.");
                return;
            }

            cart.remove(selected);
            updateTotal();
        });
    }

    private void setupClear() {

        view.getClearButton().setOnAction(event -> {
            cart.clear();
            updateTotal();
        });
    }

    // =========================================================
    // CHECKOUT
    // =========================================================

    private void setupCheckout() {
        view.getCheckoutButton().setOnAction(event -> checkout());
    }

    private void checkout() {

        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Add at least one medicine to the cart.");
            return;
        }

        // optional confirmation (Settings -> "Ask for confirmation ...")

        if (settingsService.isConfirmSale()) {

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

            confirm.setTitle("Confirm Sale");
            confirm.setHeaderText(null);
            confirm.setContentText("Complete this sale?"
                    + "\nItems: " + cart.size()
                    + "\nTotal: " + settingsService.formatMoney(getCartTotal()));

            ButtonType answer = confirm.showAndWait().orElse(ButtonType.CANCEL);

            if (answer != ButtonType.OK) {
                return;
            }
        }

        // Copy everything the background thread needs BEFORE starting it.
        // The background thread must never touch the cart or any UI control.

        final List<Sale> items = new ArrayList<>(cart);
        final double total = getCartTotal();

        // =================================================
        // BACKGROUND TASK
        // =================================================

        Task<String> checkoutTask = new Task<String>() {

            @Override
            protected String call() {

                // only one cashier at a time can read-check-write the file
                // (the same lock Add / Edit medicine use)

                synchronized (MedicineService.class) {
                    return salesService.checkout(items);
                }
            }
        };

        view.getCheckoutButton().setDisable(true);

        // =================================================
        // TASK SUCCEEDED (runs on the JavaFX thread)
        // =================================================

        checkoutTask.setOnSucceeded(event -> {

            view.getCheckoutButton().setDisable(false);

            String error = checkoutTask.getValue();

            // reload stock from file: it changed if the sale worked, and
            // it may have been changed by another cashier if it failed

            reloadMedicines();

            if (error != null) {
                showAlert(Alert.AlertType.ERROR, "Checkout Failed", error);
                return;
            }

            cart.clear();
            updateTotal();

            showAlert(Alert.AlertType.INFORMATION, "Sale Completed",
                    "Cashier: " + cashierName
                            + "\nItems: " + items.size()
                            + "\nTotal: " + settingsService.formatMoney(total));
        });

        // =================================================
        // TASK FAILED (call() threw an exception)
        // =================================================

        checkoutTask.setOnFailed(event -> {

            view.getCheckoutButton().setDisable(false);

            Throwable error = checkoutTask.getException();

            if (error != null) {
                error.printStackTrace();
            }

            reloadMedicines();

            showAlert(Alert.AlertType.ERROR, "Error",
                    "An error occurred during the sale.");
        });

        // =================================================
        // START BACKGROUND THREAD
        // =================================================

        Thread cashierThread = new Thread(checkoutTask);

        cashierThread.setDaemon(true);   // don't keep the app alive after the window closes

        cashierThread.start();
    }

    // =========================================================
    // SIDEBAR: HOME (back to the main screen = close this window)
    // =========================================================

    private void setupHomeButton() {

        view.getHomeButton().setOnAction(event -> {

            if (!cart.isEmpty()) {

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

                alert.setTitle("Home");
                alert.setHeaderText(null);
                alert.setContentText("The cart is not empty and will be cleared. Go back to the main screen?");

                if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }

            view.getRoot().getScene().getWindow().hide();
        });
    }

    // =========================================================
    // SIDEBAR: MEDICINES
    // =========================================================

    private void setupMedicinesButton() {

        view.getMedicinesButton().setOnAction(event -> {

            MedicineListView listView =
                    new MedicineListView(medicineService.loadMedicines());

            listView.show();
        });
    }

    // =========================================================
    // SIDEBAR: SALES (we are already here -> just refresh stock)
    // =========================================================

    private void setupSalesButton() {
        view.getSalesButton().setOnAction(event -> reloadMedicines());
    }

    // =========================================================
    // SIDEBAR: REPORTS
    // =========================================================

    private void setupReportsButton() {

        view.getReportsButton().setOnAction(event -> {

            ReportsView reportsView =
                    new ReportsView(
                            medicineService.loadMedicines(),
                            MedicineController.loadSalesSafely(salesService),
                            settingsService.getLowStockLimit(),
                            settingsService.getCurrency()
                    );

            reportsView.getCloseButton()
                    .setOnAction(e -> reportsView.getStage().close());

            reportsView.show();
        });
    }

    // =========================================================
    // SIDEBAR: ADD MEDICINE
    // =========================================================

    private void setupAddButton() {
        view.getAddButton().setOnAction(event -> addMedicine());
    }

    private void addMedicine() {

        AddMedicineView addView = new AddMedicineView();

        addView.getSaveButton()
                .setOnAction(event -> saveNewMedicine(addView));

        addView.getCancelButton()
                .setOnAction(event -> addView.getStage().close());

        addView.show();
    }

    private void saveNewMedicine(AddMedicineView addView) {

        Medicine medicine = readMedicine(
                addView.getNameField().getText(),
                addView.getPriceField().getText(),
                addView.getQuantityField().getText()
        );

        if (medicine == null) {
            return;
        }

        MedicineController.runInBackground(
                addView.getSaveButton(),
                () -> MedicineController.addMedicineSafely(medicineService, medicine),
                error -> finishSave(addView.getStage(), error, "Medicine added successfully.")
        );
    }

    // called on the JavaFX thread when a save finished (error == null -> success)

    private void finishSave(Stage stage, String error, String successMessage) {

        // always show the real data: the file may have been changed by someone else

        reloadMedicines();

        stage.close();

        if (error == null) {
            showAlert(Alert.AlertType.INFORMATION, "Success", successMessage);
        } else {
            showAlert(Alert.AlertType.WARNING, "Not Applied", error);
        }
    }

    // =========================================================
    // SIDEBAR: EDIT MEDICINE
    // =========================================================

    private void setupEditButton() {
        view.getEditButton().setOnAction(event -> editSelectedMedicine());
    }

    private void editSelectedMedicine() {

        Medicine selected = view.getMedicineTable().getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Medicine Selected", "Please select a medicine first.");
            return;
        }

        EditMedicineView editView = new EditMedicineView(selected);

        editView.getSaveButton()
                .setOnAction(event -> saveEditedMedicine(selected, editView));

        editView.getCancelButton()
                .setOnAction(event -> editView.getStage().close());

        editView.show();
    }

    private void saveEditedMedicine(Medicine oldMedicine, EditMedicineView editView) {

        Medicine updated = readMedicine(
                editView.getNameField().getText(),
                editView.getPriceField().getText(),
                editView.getQuantityField().getText()
        );

        if (updated == null) {
            return;
        }

        MedicineController.runInBackground(
                editView.getSaveButton(),
                () -> MedicineController.updateMedicineSafely(
                        medicineService, oldMedicine, updated),
                error -> {

                    if (error == null) {

                        // the cart line still holds the old name/price -> remove it

                        Sale staleLine = findInCart(oldMedicine.getName());

                        if (staleLine != null) {
                            cart.remove(staleLine);
                            updateTotal();
                        }
                    }

                    finishSave(editView.getStage(), error, "Medicine updated successfully.");
                }
        );
    }

    // =========================================================
    // SIDEBAR: SETTINGS
    // =========================================================

    private void setupSettingsButton() {
        view.getSettingsButton().setOnAction(event -> openSettings());
    }

    private void loadSavedSettings() {

        view.updateHeaderInfo(
                settingsService.getPharmacyName(),
                settingsService.getDisplayName(cashierName)
        );

        view.updateSubtitle(settingsService.getSubtitle());

        // the currency may have changed -> redraw the total

        updateTotal();
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

        settingsView.getCancelButton()
                .setOnAction(e -> settingsView.getStage().close());

        settingsView.show();
    }

    private void saveSettings(SettingsView settingsView) {

        boolean passwordChanged =
                !settingsView.getNewPasswordField().getText().trim().isEmpty();

        // validation + saving + password change all happen in SettingsService

        String error = settingsService.saveSettings(
                cashierName,
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
            showAlert(Alert.AlertType.WARNING, "Invalid Data", error);
            return;
        }

        loadSavedSettings();

        settingsView.getStage().close();

        showAlert(Alert.AlertType.INFORMATION, "Success", passwordChanged
                ? "Settings saved and password changed."
                : "Settings saved successfully.");
    }

    // =========================================================
    // SIDEBAR: LOGOUT
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
    // HELPERS
    // =========================================================

    /**
     * Validates the form values and builds a Medicine.
     * Shows an alert and returns null if anything is invalid.
     */
    private Medicine readMedicine(String nameText, String priceText, String quantityText) {

        String name = nameText.trim();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Medicine name cannot be empty.");
            return null;
        }

        double price;
        int quantity;

        try {
            price = Double.parseDouble(priceText.trim());
            quantity = Integer.parseInt(quantityText.trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data",
                    "Price must be a number and quantity must be an integer.");
            return null;
        }

        if (price < 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Price cannot be negative.");
            return null;
        }

        if (quantity < 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Quantity cannot be negative.");
            return null;
        }

        return new Medicine(name, price, quantity);
    }

    private void reloadMedicines() {
        allMedicines.setAll(medicineService.loadMedicines());
    }

    private double getCartTotal() {

        double total = 0;

        for (Sale sale : cart) {
            total += sale.getTotal();
        }

        return total;
    }

    private void updateTotal() {
        view.getTotalLabel().setText("Total: " + settingsService.formatMoney(getCartTotal()));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public SalesView getView() {
        return view;
    }
}
