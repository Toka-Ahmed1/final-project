package sample.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomeView {

    // =========================
    // Main Root
    // =========================

    private final StackPane root;

    // =========================
    // Controls
    // =========================

    private TextField usernameField;
    private PasswordField passwordField;

    private Button loginButton;

    private Label messageLabel;

    public HomeView() {

        root = new StackPane();

        root.getChildren().add(createLoginCard());
    }

    // =========================================================
    // LOGIN CARD
    // =========================================================

    private VBox createLoginCard() {

        // -------------------------
        // Branding
        // -------------------------

        Label logo = new Label("FCI PHARMA CARE");
        Label subtitle = new Label("Pharmacy Management System");

        VBox branding = new VBox(3, logo, subtitle);

        branding.setAlignment(Pos.CENTER);

        // -------------------------
        // Fields
        // -------------------------

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefHeight(40);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(40);

        // -------------------------
        // Error / info message
        // -------------------------

        messageLabel = new Label();
        messageLabel.setWrapText(true);

        // -------------------------
        // Login button
        // -------------------------

        loginButton = new Button("Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(42);
        loginButton.setDefaultButton(true);   // Enter key presses it

        // -------------------------
        // Card
        // -------------------------

        VBox card = new VBox(15,
                branding,
                usernameField,
                passwordField,
                messageLabel,
                loginButton
        );

        card.setPadding(new Insets(30));
        card.setMaxWidth(360);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER);

        return card;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public StackPane getRoot() {
        return root;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Label getMessageLabel() {
        return messageLabel;
    }
}