package sample.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import sample.model.Medicine;
import sample.model.Sale;

public class SalesView {

    // =========================
    // Main Root
    // =========================

    private final BorderPane root;

    // =========================
    // Logged-in user (from Login)
    // =========================

    private final String username;

    // =========================
    // Controls
    // =========================

    private TextField searchField;
    private TextField quantityField;

    private TableView<Medicine> medicineTable;
    private TableView<Sale> cartTable;

    private Button addToCartButton;
    private Button removeButton;
    private Button clearButton;
    private Button checkoutButton;

    private Label totalLabel;

    // Header labels (updated from Settings)
    private Label logoLabel;
    private Label subtitleLabel;
    private Label userLabel;

    // Sidebar buttons (same as MedicineView)

    private Button homeButton;
    private Button medicinesButton;
    private Button salesButton;
    private Button reportsButton;
    private Button addButton;
    private Button editButton;
    private Button settingsButton;
    private Button logoutButton;

    public SalesView(String username) {

        // must be set BEFORE createLayout(), the header uses it
        this.username = username;

        root = new BorderPane();

        createLayout();
    }

    // =========================
    // Main Layout
    // =========================

    private void createLayout() {

        root.setTop(createHeader());

        root.setLeft(createSidebar());

        root.setCenter(createMainContent());
    }

    // =========================================================
    // HEADER
    // =========================================================

    private HBox createHeader() {

        logoLabel = new Label("FCI PHARMA CARE");
        subtitleLabel = new Label("Pharmacy Management System");

        VBox branding = new VBox(3, logoLabel, subtitleLabel);

        userLabel = new Label(username + "  ● Online");

        HBox header = new HBox(20, branding, userLabel);

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));

        HBox.setHgrow(branding, Priority.ALWAYS);

        return header;
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(10);

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);

        // -------------------------
        // Dashboard Section
        // -------------------------

        Label dashboardTitle = new Label("DASHBOARD");

        homeButton = createMenuButton("🏠  Home");
        medicinesButton = createMenuButton("💊  Medicines");
        salesButton = createMenuButton("🛒  Sales");
        reportsButton = createMenuButton("📊  Reports");

        // -------------------------
        // Management Section
        // -------------------------

        Label managementTitle = new Label("MANAGEMENT");

        addButton = createMenuButton("➕  Add Medicine");
        editButton = createMenuButton("✏  Edit Medicine");
        settingsButton = createMenuButton("⚙  Settings");

        // -------------------------
        // Spacer
        // -------------------------

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // -------------------------
        // Logout
        // -------------------------

        logoutButton = createMenuButton("🚪  Logout");

        // -------------------------
        // Add everything
        // -------------------------

        sidebar.getChildren().addAll(

                dashboardTitle,

                homeButton,
                medicinesButton,
                salesButton,
                reportsButton,

                managementTitle,

                addButton,
                editButton,
                settingsButton,

                spacer,

                logoutButton
        );

        return sidebar;
    }

    // =========================================================
    // MENU BUTTON
    // =========================================================

    private Button createMenuButton(String text) {

        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(42);
        button.setAlignment(Pos.CENTER_LEFT);

        return button;
    }

    // =========================================================
    // MAIN CONTENT
    // =========================================================

    private VBox createMainContent() {

        VBox content = new VBox(15);

        content.setPadding(new Insets(30));

        // -------------------------
        // Page Title
        // -------------------------

        Label title = new Label("New Sale");
        Label description = new Label("Select medicines, add them to the cart, then checkout");

        VBox titleSection = new VBox(5, title, description);

        // -------------------------
        // Search
        // -------------------------

        searchField = new TextField();
        searchField.setPromptText("Search medicine name...");
        searchField.setPrefHeight(40);

        // -------------------------
        // Medicines Table
        // -------------------------

        medicineTable = createMedicineTable();

        // -------------------------
        // Add To Cart Row
        // -------------------------

        quantityField = new TextField("1");
        quantityField.setPrefWidth(80);
        quantityField.setPromptText("Qty");

        addToCartButton = new Button("🛒  Add to Cart");

        HBox addRow = new HBox(10, new Label("Quantity:"), quantityField, addToCartButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        // -------------------------
        // Cart
        // -------------------------

        Label cartTitle = new Label("Cart");

        cartTable = createCartTable();

        // -------------------------
        // Bottom Bar
        // -------------------------

        removeButton = new Button("Remove Selected");
        clearButton = new Button("Clear Cart");
        checkoutButton = new Button("✔  Checkout");

        totalLabel = new Label("Total: 0.00");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(10, removeButton, clearButton, spacer, totalLabel, checkoutButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        // -------------------------
        // Add Components
        // -------------------------

        content.getChildren().addAll(
                titleSection,
                searchField,
                medicineTable,
                addRow,
                cartTitle,
                cartTable,
                bottomBar
        );

        VBox.setVgrow(medicineTable, Priority.ALWAYS);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        return content;
    }

    // =========================================================
    // MEDICINE TABLE
    // =========================================================

    private TableView<Medicine> createMedicineTable() {

        TableView<Medicine> table = new TableView<>();

        TableColumn<Medicine, String> nameColumn = new TableColumn<>("Medicine");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Medicine, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Medicine, Integer> stockColumn = new TableColumn<>("Stock");
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        table.getColumns().addAll(nameColumn, priceColumn, stockColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return table;
    }

    // =========================================================
    // CART TABLE
    // =========================================================

    private TableView<Sale> createCartTable() {

        TableView<Sale> table = new TableView<>();

        TableColumn<Sale, String> nameColumn = new TableColumn<>("Medicine");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("medicineName"));

        TableColumn<Sale, Double> priceColumn = new TableColumn<>("Unit Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<Sale, Integer> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Sale, Double> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));

        table.getColumns().addAll(nameColumn, priceColumn, quantityColumn, totalColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return table;
    }

    // =========================================================
    // GETTERS - CONTENT
    // =========================================================

    public BorderPane getRoot() { return root; }

    public TextField getSearchField() { return searchField; }

    public TextField getQuantityField() { return quantityField; }

    public TableView<Medicine> getMedicineTable() { return medicineTable; }

    public TableView<Sale> getCartTable() { return cartTable; }

    public Button getAddToCartButton() { return addToCartButton; }

    public Button getRemoveButton() { return removeButton; }

    public Button getClearButton() { return clearButton; }

    public Button getCheckoutButton() { return checkoutButton; }

    public Label getTotalLabel() { return totalLabel; }

    public void updateSubtitle(String text) {

        if (text != null && !text.isBlank()) {
            subtitleLabel.setText(text);
        }
    }

    public void updateHeaderInfo(String pharmacyName, String userName) {

        if (pharmacyName != null && !pharmacyName.isBlank()) {
            logoLabel.setText(pharmacyName);
        }

        if (userName != null && !userName.isBlank()) {
            userLabel.setText(userName + "  ● Online");
        }
    }



    // =========================================================
    // GETTERS - SIDEBAR (same as MedicineView)
    // =========================================================

    public Button getHomeButton() { return homeButton; }

    public Button getMedicinesButton() { return medicinesButton; }

    public Button getSalesButton() { return salesButton; }

    public Button getReportsButton() { return reportsButton; }

    public Button getAddButton() { return addButton; }

    public Button getEditButton() { return editButton; }

    public Button getSettingsButton() { return settingsButton; }

    public Button getLogoutButton() { return logoutButton; }
}
