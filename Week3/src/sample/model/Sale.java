package sample.model;

import java.time.LocalDateTime;

public class Sale {

    private final String medicineName;
    private final double unitPrice;
    private int quantity;
    private LocalDateTime dateTime;

    public Sale(String medicineName, double unitPrice, int quantity) {
        this(medicineName, unitPrice, quantity, LocalDateTime.now());
    }

    public Sale(String medicineName, double unitPrice, int quantity, LocalDateTime dateTime) {
        this.medicineName = medicineName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.dateTime = dateTime;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotal() {
        return unitPrice * quantity;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}

