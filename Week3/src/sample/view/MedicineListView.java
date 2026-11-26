package sample.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sample.model.Medicine;

import java.util.List;

// Window that displays all available medicines in a table
public class MedicineListView {

    private final Stage stage;

    private final TableView<Medicine> table;

    private final Button closeButton;

    public MedicineListView(List<Medicine> medicines) {

        stage = new Stage();
        stage.setTitle("Medicines");

        // =========================
        // Title
        // =========================

        Label title = new Label("All Medicines");

        Label countLabel =
                new Label("Total: " + medicines.size() + " medicines");

        // =========================
        // Table
        // =========================

        table = new TableView<>();

        TableColumn<Medicine, String> nameColumn =
                new TableColumn<>("Medicine");
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));

        TableColumn<Medicine, Double> priceColumn =
                new TableColumn<>("Price");
        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>("price"));

        TableColumn<Medicine, Integer> quantityColumn =
                new TableColumn<>("Stock");
        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity"));

        TableColumn<Medicine, String> statusColumn =
                new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> {

            int quantity = data.getValue().getQuantity();

            String status;

            if (quantity == 0) {
                status = "● Out of Stock";
            } else if (quantity <= 10) {
                status = "● Low Stock";
            } else {
                status = "● Available";
            }

            return new SimpleStringProperty(status);
        });

        table.getColumns().addAll(
                nameColumn,
                priceColumn,
                quantityColumn,
                statusColumn
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        table.setItems(
                FXCollections.observableArrayList(medicines)
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        // =========================
        // Close button
        // =========================

        closeButton = new Button("Close");

        closeButton.setOnAction(event -> stage.close());

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getChildren().add(closeButton);

        // =========================
        // Root
        // =========================

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.getChildren().addAll(
                title,
                countLabel,
                table,
                buttons
        );

        stage.setScene(new Scene(root, 600, 450));
    }

    public Stage getStage() {
        return stage;
    }

    public TableView<Medicine> getTable() {
        return table;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void show() {
        stage.show();
    }
}
