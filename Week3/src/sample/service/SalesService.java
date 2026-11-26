package sample.service;

import sample.model.Medicine;
import sample.model.Sale;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SalesService {

    // medicines live here (MedicineService)
    private final String medicinesPath = "Week3/Data/medicines.txt";

    // sales live in their OWN file (they used to be mixed into medicines.txt)
    private final String salesPath = "Week3/Data/sales.txt";

    private final MedicineService medicineService = new MedicineService();

    // =========================================================
    // LOAD SALES   (format: name,unitPrice,quantity,dateTime)
    // Useful for ReportsView
    // =========================================================

    public List<Sale> loadSales() {

        // sales saved by the old version are moved out of medicines.txt first

        migrateOldSales();

        List<Sale> sales = new ArrayList<>();

        File file = new File(salesPath);

        if (!file.exists()) {
            return sales;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                Sale sale = parseSale(line);

                if (sale != null) {
                    sales.add(sale);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return sales;
    }

    // returns null if the line is not a valid sale line
    // (a medicine line has only 3 fields and no date)

    private Sale parseSale(String line) {

        if (line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",");

        if (parts.length < 4) {
            return null;
        }

        try {
            return new Sale(
                    parts[0].trim(),
                    Double.parseDouble(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    LocalDateTime.parse(parts[3].trim())
            );

        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    // =========================================================
    // ONE-TIME CLEANUP
    // moves the old sale lines from medicines.txt to sales.txt
    // (a copy of the original is kept in medicines.txt.bak)
    // =========================================================

    private void migrateOldSales() {

        synchronized (MedicineService.class) {

            File medicinesFile = new File(medicinesPath);

            if (!medicinesFile.exists()) {
                return;
            }

            List<String> medicineLines = new ArrayList<>();
            List<String> saleLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(medicinesFile))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    if (parseSale(line) != null) {
                        saleLines.add(line.trim());
                    } else {
                        medicineLines.add(line);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            if (saleLines.isEmpty()) {
                return;   // nothing to move
            }

            try {

                // 1) backup  2) save the sales  3) clean medicines.txt

                Files.copy(
                        medicinesFile.toPath(),
                        Path.of(medicinesPath + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING
                );

                appendLines(saleLines);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(medicinesFile))) {

                    for (String line : medicineLines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void appendLines(List<String> lines) throws IOException {

        File file = new File(salesPath);

        File folder = file.getParentFile();

        if (folder != null) {
            folder.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // =========================================================
    // CHECKOUT
    // returns null if success, otherwise an error message
    // =========================================================

    public String checkout(List<Sale> items) {

        // old sale lines must not stay inside medicines.txt while we edit it

        migrateOldSales();

        List<Medicine> stock = medicineService.loadMedicines();

        // 1) Validate everything first (all or nothing)

        for (Sale item : items) {

            Medicine medicine = findMedicine(stock, item.getMedicineName());

            if (medicine == null) {
                return item.getMedicineName() + " no longer exists.";
            }

            if (item.getQuantity() > medicine.getQuantity()) {
                return "Not enough stock for " + item.getMedicineName()
                        + " (available: " + medicine.getQuantity() + ").";
            }
        }

        // 2) Deduct stock

        for (Sale item : items) {

            Medicine medicine = findMedicine(stock, item.getMedicineName());

            medicineService.updateMedicine(
                    medicine,
                    new Medicine(
                            medicine.getName(),
                            medicine.getPrice(),
                            medicine.getQuantity() - item.getQuantity()
                    )
            );
        }

        // 3) Save the sale lines with one shared timestamp (in sales.txt)

        LocalDateTime now = LocalDateTime.now();

        List<String> saleLines = new ArrayList<>();

        for (Sale item : items) {

            item.setDateTime(now);

            saleLines.add(item.getMedicineName() + ","
                    + item.getUnitPrice() + ","
                    + item.getQuantity() + ","
                    + item.getDateTime());
        }

        try {
            appendLines(saleLines);

        } catch (IOException e) {
            e.printStackTrace();
            return "Could not save the sale.";
        }

        return null;
    }

    private Medicine findMedicine(List<Medicine> list, String name) {

        for (Medicine medicine : list) {
            if (medicine.getName().equals(name)) {
                return medicine;
            }
        }

        return null;
    }
}
