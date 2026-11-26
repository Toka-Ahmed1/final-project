package sample.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsView {

    private final Stage stage;

    // pharmacy info
    private final TextField pharmacyNameField;
    private final TextField addressField;
    private final TextField phoneField;

    // preferences
    private final TextField userNameField;
    private final TextField currencyField;
    private final TextField lowStockField;
    private final CheckBox confirmSaleCheck;

    // change password
    private final PasswordField currentPasswordField;
    private final PasswordField newPasswordField;
    private final PasswordField confirmPasswordField;

    private final Button saveButton;
    private final Button cancelButton;

    public SettingsView() {

        stage = new Stage();

        stage.setTitle("Settings");

        stage.initModality(Modality.APPLICATION_MODAL);

        // =========================
        // Fields
        // =========================

        pharmacyNameField = new TextField();
        pharmacyNameField.setPromptText("Pharmacy name");

        addressField = new TextField();
        addressField.setPromptText("e.g. 12 Tahrir St, Cairo");

        phoneField = new TextField();
        phoneField.setPromptText("e.g. 01012345678");

        userNameField = new TextField();
        userNameField.setPromptText("Leave empty to use the login name");

        currencyField = new TextField();
        currencyField.setPromptText("e.g. EGP");

        lowStockField = new TextField();
        lowStockField.setPromptText("e.g. 10");

        confirmSaleCheck = new CheckBox("Ask for confirmation before completing a sale");

        currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");

        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Repeat new password");

        // =========================
        // Sections
        // =========================

        GridPane pharmacyForm = createForm();

        addRow(pharmacyForm, 0, "Pharmacy Name", pharmacyNameField);
        addRow(pharmacyForm, 1, "Address", addressField);
        addRow(pharmacyForm, 2, "Phone", phoneField);

        GridPane preferencesForm = createForm();

        addRow(preferencesForm, 0, "Display Name", userNameField);
        addRow(preferencesForm, 1, "Currency", currencyField);
        addRow(preferencesForm, 2, "Low Stock Limit", lowStockField);

        preferencesForm.add(confirmSaleCheck, 1, 3);

        GridPane passwordForm = createForm();

        addRow(passwordForm, 0, "Current Password", currentPasswordField);
        addRow(passwordForm, 1, "New Password", newPasswordField);
        addRow(passwordForm, 2, "Confirm Password", confirmPasswordField);

        Label passwordHint = new Label("Leave all three empty to keep the current password.");

        VBox content = new VBox(15,
                new Label("Settings"),

                new Label("PHARMACY"),
                pharmacyForm,

                new Label("PREFERENCES"),
                preferencesForm,

                new Label("CHANGE PASSWORD"),
                passwordForm,
                passwordHint
        );

        content.setPadding(new Insets(25));

        ScrollPane scroll = new ScrollPane(content);

        scroll.setFitToWidth(true);

        // =========================
        // Buttons (always visible)
        // =========================

        saveButton = new Button("Save Settings");
        cancelButton = new Button("Cancel");

        HBox buttons = new HBox(10, saveButton, cancelButton);

        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 25, 15, 25));

        // =========================
        // Root
        // =========================

        BorderPane root = new BorderPane();

        root.setCenter(scroll);
        root.setBottom(buttons);

        stage.setScene(new Scene(root, 540, 680));
    }

    // =========================
    // Helpers
    // =========================

    private GridPane createForm() {

        GridPane form = new GridPane();

        form.setHgap(15);
        form.setVgap(12);

        return form;
    }

    private void addRow(GridPane form, int row, String labelText, TextField field) {

        form.add(new Label(labelText), 0, row);
        form.add(field, 1, row);

        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    // =========================
    // Getters
    // =========================

    public Stage getStage() {
        return stage;
    }

    public TextField getPharmacyNameField() {
        return pharmacyNameField;
    }

    public TextField getAddressField() {
        return addressField;
    }

    public TextField getPhoneField() {
        return phoneField;
    }

    public TextField getUserNameField() {
        return userNameField;
    }

    public TextField getCurrencyField() {
        return currencyField;
    }

    public TextField getLowStockField() {
        return lowStockField;
    }

    public CheckBox getConfirmSaleCheck() {
        return confirmSaleCheck;
    }

    public PasswordField getCurrentPasswordField() {
        return currentPasswordField;
    }

    public PasswordField getNewPasswordField() {
        return newPasswordField;
    }

    public PasswordField getConfirmPasswordField() {
        return confirmPasswordField;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public void show() {
        stage.showAndWait();
    }
}
