package sample.service;

import sample.model.Medicine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MedicineService {

    public static final Object MEDICINE_LOCK = new Object();

    private final String filePath = "Week3/Data/medicines.txt";

    // =====================================================
    // LOAD MEDICINES
    // =====================================================

    public List<Medicine> loadMedicines() {
        return loadMedicines(true);
    }

    public List<Medicine> loadMedicines(boolean useSync) {
        if (useSync) {
            synchronized (MEDICINE_LOCK) {
                return readMedicinesFromFile();
            }
        }
        return readMedicinesFromFile();
    }

    private List<Medicine> readMedicinesFromFile() {
        List<Medicine> medicines = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(",");

                if (parts.length != 3) {
                    continue;
                }

                String name = parts[0].trim();
                double price = Double.parseDouble(parts[1].trim());
                int quantity = Integer.parseInt(parts[2].trim());

                medicines.add(
                        new Medicine(name, price, quantity)
                );
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return medicines;
    }

    // =====================================================
    // ADD MEDICINE
    // =====================================================

    public void addMedicine(Medicine medicine) {
        addMedicine(medicine, true);
    }

    public void addMedicine(Medicine medicine, boolean useSync) {
        if (useSync) {
            synchronized (MEDICINE_LOCK) {
                System.out.println("[SYNC ADD] Acquired lock: " + Thread.currentThread().getName());
                appendMedicineToFile(medicine);
                System.out.println("[SYNC ADD] Releasing lock: " + Thread.currentThread().getName());
            }
        } else {
            System.out.println("[NO-SYNC ADD] Running without lock: " + Thread.currentThread().getName());
            appendMedicineToFile(medicine);
        }
    }

    private void appendMedicineToFile(Medicine medicine) {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(filePath, true))) {

            writer.write(
                    medicine.getName() + ","
                            + medicine.getPrice() + ","
                            + medicine.getQuantity()
            );

            writer.newLine();

        } catch (IOException e) {
            throw new RuntimeException("Failed to append medicine to file", e);
        }
    }

    // =====================================================
    // UPDATE MEDICINE
    // =====================================================

    public void updateMedicine(
            Medicine oldMedicine,
            Medicine updatedMedicine) {
        updateMedicine(oldMedicine, updatedMedicine, true);
    }

    public void updateMedicine(
            Medicine oldMedicine,
            Medicine updatedMedicine,
            boolean useSync) {

        if (useSync) {
            synchronized (MEDICINE_LOCK) {
                System.out.println("[SYNC UPDATE] Acquired lock: " + Thread.currentThread().getName());
                performUpdateMedicine(oldMedicine, updatedMedicine);
                System.out.println("[SYNC UPDATE] Releasing lock: " + Thread.currentThread().getName());
            }
        } else {
            System.out.println("[NO-SYNC UPDATE] Running without lock: " + Thread.currentThread().getName());
            performUpdateMedicine(oldMedicine, updatedMedicine);
        }
    }

    private void performUpdateMedicine(
            Medicine oldMedicine,
            Medicine updatedMedicine) {

        List<Medicine> medicines = readMedicinesFromFile();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Update interrupted", e);
        }

        boolean found = false;

        for (int i = 0; i < medicines.size(); i++) {
            Medicine current = medicines.get(i);

            if (current.getName().equalsIgnoreCase(oldMedicine.getName())) {
                medicines.set(i, updatedMedicine);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException(
                    "Update rejected: '" + oldMedicine.getName()
                            + "' was already modified or removed by another operation."
            );
        }

        try {
            saveMedicines(medicines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save updated medicines", e);
        }
    }

    // Without Sync

    public boolean sellMedicineWithoutSync(
            String medicineName,
            int requestedQuantity) {

        if (requestedQuantity <= 0) {
            return false;
        }

        List<Medicine> medicines = readMedicinesFromFile();

        for (int i = 0; i < medicines.size(); i++) {

            Medicine medicine = medicines.get(i);

            if (medicine.getName().equalsIgnoreCase(medicineName)) {

                if (medicine.getQuantity() < requestedQuantity) {
                    return false;
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }

                int remainingQuantity =
                        medicine.getQuantity() - requestedQuantity;

                medicines.set(i, new Medicine(
                        medicine.getName(),
                        medicine.getPrice(),
                        remainingQuantity
                ));

                try {
                    saveMedicines(medicines);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return false;
    }

    // =====================================================
    // SELL MEDICINE with sync
    // =====================================================

    public boolean sellMedicine(
            String medicineName,
            int requestedQuantity) {

        synchronized (MEDICINE_LOCK) {
            if (requestedQuantity <= 0) {
                return false;
            }

            List<Medicine> medicines = readMedicinesFromFile();

            for (int i = 0; i < medicines.size(); i++) {

                Medicine medicine = medicines.get(i);

                if (medicine.getName().equalsIgnoreCase(medicineName)) {

                    if (medicine.getQuantity() < requestedQuantity) {
                        return false;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }

                    int remainingQuantity =
                            medicine.getQuantity() - requestedQuantity;

                    Medicine updatedMedicine = new Medicine(
                            medicine.getName(),
                            medicine.getPrice(),
                            remainingQuantity
                    );

                    medicines.set(i, updatedMedicine);

                    try {
                        saveMedicines(medicines);
                        return true;

                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }

            return false;
        }
    }

    // =====================================================
    // SAVE ALL MEDICINES
    // =====================================================

    private void saveMedicines(
            List<Medicine> medicines) throws IOException {

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(filePath))) {

            for (Medicine medicine : medicines) {

                writer.write(
                        medicine.getName() + ","
                                + medicine.getPrice() + ","
                                + medicine.getQuantity()
                );

                writer.newLine();
            }
        }
    }
}