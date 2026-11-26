package sample.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import sample.model.Medicine;
import sample.model.Sale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsView {

    private final Stage stage;
    private final Button closeButton;
    private final String currency;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public ReportsView(List<Medicine> medicines, int lowStockLimit) {
        this(medicines, new ArrayList<>(), lowStockLimit, "");
    }

    public ReportsView(List<Medicine> medicines, int lowStockLimit, String currency) {
        this(medicines, new ArrayList<>(), lowStockLimit, currency);
    }

    public ReportsView(List<Medicine> medicines, List<Sale> sales, int lowStockLimit) {
        this(medicines, sales, lowStockLimit, "");
    }

    public ReportsView(List<Medicine> medicines, List<Sale> sales, int lowStockLimit, String currency) {

        this.currency = (currency == null) ? "" : currency.trim();

        stage = new Stage();
        stage.setTitle("Reports");
        stage.initModality(Modality.APPLICATION_MODAL);

        TabPane tabs = new TabPane();

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                new Tab("📦  Stock Report", createStockTab(medicines, lowStockLimit)),
                new Tab("💰  Sales Report", createSalesTab(sales))
        );

        closeButton = new Button("Close");

        HBox bottom = new HBox(closeButton);

        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(10, 20, 15, 20));

        BorderPane root = new BorderPane();

        root.setCenter(tabs);
        root.setBottom(bottom);

        stage.setScene(new Scene(root, 800, 560));
    }

    // =========================================================
    // STOCK TAB
    // =========================================================

    private VBox createStockTab(List<Medicine> medicines, int lowStockLimit) {

        int outOfStock = 0;
        int lowStock = 0;
        double inventoryValue = 0;

        List<Medicine> needsAttention = new ArrayList<>();

        for (Medicine medicine : medicines) {

            inventoryValue += medicine.getPrice() * medicine.getQuantity();

            if (medicine.getQuantity() == 0) {
                outOfStock++;
                needsAttention.add(medicine);
            } else if (medicine.getQuantity() <= lowStockLimit) {
                lowStock++;
                needsAttention.add(medicine);
            }
        }

        // lowest stock first
        needsAttention.sort((a, b) -> Integer.compare(a.getQuantity(), b.getQuantity()));

        HBox cards = new HBox(15,
                createCard("Total Medicines", String.valueOf(medicines.size())),
                createCard("Low Stock (≤ " + lowStockLimit + ")", String.valueOf(lowStock)),
                createCard("Out of Stock", String.valueOf(outOfStock)),
                createCard("Inventory Value", money(inventoryValue))
        );

        Label tableTitle = new Label("Medicines that need restocking");

        TableView<Medicine> table = new TableView<>();

        TableColumn<Medicine, String> nameColumn = new TableColumn<>("Medicine");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Medicine, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Medicine, Integer> stockColumn = new TableColumn<>("Stock");
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        table.getColumns().addAll(nameColumn, priceColumn, stockColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getItems().addAll(needsAttention);
        table.setPlaceholder(new Label("Everything is well stocked 👍"));

        return createTab(cards, tableTitle, table);
    }

    // =========================================================
    // SALES TAB
    // =========================================================

    private VBox createSalesTab(List<Sale> sales) {

        double revenue = 0;
        int unitsSold = 0;

        Map<String, Integer> unitsPerMedicine = new HashMap<>();

        if (sales != null) {
            for (Sale sale : sales) {
                revenue += sale.getTotal();
                unitsSold += sale.getQuantity();
                unitsPerMedicine.merge(sale.getMedicineName(), sale.getQuantity(), Integer::sum);
            }
        }

        String bestSeller = "-";
        int bestUnits = 0;

        for (Map.Entry<String, Integer> entry : unitsPerMedicine.entrySet()) {
            if (entry.getValue() > bestUnits) {
                bestUnits = entry.getValue();
                bestSeller = entry.getKey();
            }
        }

        HBox cards = new HBox(15,
                createCard("Total Revenue", money(revenue)),
                createCard("Units Sold", String.valueOf(unitsSold)),
                createCard("Best Seller", bestSeller)
        );

        Label tableTitle = new Label("Sales history");

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
        if (sales != null) {
            table.getItems().addAll(sales);
        }
        table.setPlaceholder(new Label("No sales yet"));

        return createTab(cards, tableTitle, table);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private VBox createTab(HBox cards, Label tableTitle, TableView<?> table) {

        VBox content = new VBox(15, cards, tableTitle, table);

        content.setPadding(new Insets(20));

        VBox.setVgrow(table, Priority.ALWAYS);

        return content;
    }

    private String money(double amount) {

        String number = String.format(java.util.Locale.US, "%.2f", amount);

        return currency.isEmpty() ? number : number + " " + currency;
    }

    private VBox createCard(String title, String value) {

        VBox card = new VBox(8, new Label(title), new Label(value));

        card.setPadding(new Insets(15));
        card.setPrefWidth(180);

        return card;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Stage getStage() {
        return stage;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void show() {
        stage.showAndWait();
    }
}