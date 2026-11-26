package sample.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class SettingsService {

    // same folder as medicines.txt and sales.txt (SalesService uses "Data/...")

    private static final String FILE_PATH = "-progect/Week3/Data/medicines.txt";

    private static final String DEFAULT_PHARMACY_NAME = "FCI PHARMA CARE";
    private static final String DEFAULT_SUBTITLE = "Pharmacy Management System";
    private static final String DEFAULT_CURRENCY = "EGP";
    private static final int DEFAULT_LOW_STOCK_LIMIT = 10;
    private static final int MIN_PASSWORD_LENGTH = 4;

    // accounts that work until somebody changes the password from Settings

    private static final Map<String, String> DEFAULT_ACCOUNTS = Map.of(
            "admin", "1234",
            "aseel", "5678",
            "mohammed", "9999"
    );

    private final Properties properties = new Properties();

    public SettingsService() {
        load();
    }

    // =========================================================
    // LOAD / RELOAD / STORE
    // =========================================================

    private void load() {

        properties.clear();

        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return;   // first run: defaults are used
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {

            properties.load(reader);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // read the file again (another window may have saved new settings)

    public void reload() {
        load();
    }

    private boolean store() {

        File file = new File(FILE_PATH);

        File folder = file.getParentFile();

        if (folder != null) {
            folder.mkdirs();
        }

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {

            properties.store(writer, "Pharmacy settings");

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // SAVE ALL SETTINGS (+ optional password change)
    // =========================================================

    /**
     * Validates everything, then saves.
     * Returns null when saved, otherwise the error message to show.
     * Nothing is saved if anything is invalid.
     * Leave the 3 password fields empty to keep the current password.
     */
    public String saveSettings(
            String loginName,
            String pharmacyName,
            String address,
            String phone,
            String displayName,
            String currency,
            String lowStockText,
            boolean confirmSale,
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        // pick up changes saved by another window first

        load();

        pharmacyName = pharmacyName.trim();
        address = address.trim();
        phone = phone.trim();
        displayName = displayName.trim();
        currency = currency.trim();

        // login trims the password too, so we trim it here as well

        currentPassword = currentPassword.trim();
        newPassword = newPassword.trim();
        confirmPassword = confirmPassword.trim();

        if (pharmacyName.isEmpty()) {
            return "Pharmacy name cannot be empty.";
        }

        int lowStockLimit;

        try {
            lowStockLimit = Integer.parseInt(lowStockText.trim());
        } catch (NumberFormatException e) {
            return "Low stock limit must be an integer.";
        }

        if (lowStockLimit < 0) {
            return "Low stock limit cannot be negative.";
        }

        boolean changePassword =
                !currentPassword.isEmpty()
                        || !newPassword.isEmpty()
                        || !confirmPassword.isEmpty();

        if (changePassword) {

            String error = validatePasswordChange(
                    loginName, currentPassword, newPassword, confirmPassword
            );

            if (error != null) {
                return error;
            }
        }

        properties.setProperty("pharmacyName", pharmacyName);
        properties.setProperty("address", address);
        properties.setProperty("phone", phone);
        properties.setProperty("userName", displayName);
        properties.setProperty("currency", currency);
        properties.setProperty("lowStockLimit", String.valueOf(lowStockLimit));
        properties.setProperty("confirmSale", String.valueOf(confirmSale));

        if (changePassword) {
            properties.setProperty("password." + loginName, hash(newPassword));
        }

        if (!store()) {
            return "Could not write the settings file.";
        }

        return null;
    }

    private String validatePasswordChange(
            String loginName,
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return "To change the password fill all three password fields.";
        }

        if (!checkLogin(loginName, currentPassword)) {
            return "Current password is incorrect.";
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            return "New password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "New password and confirmation do not match.";
        }

        if (newPassword.equals(currentPassword)) {
            return "New password must be different from the current one.";
        }

        return null;
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public boolean checkLogin(String username, String password) {

        String saved = properties.getProperty("password." + username);

        if (saved != null) {
            return saved.equals(hash(password));
        }

        String defaultPassword = DEFAULT_ACCOUNTS.get(username);

        return defaultPassword != null && defaultPassword.equals(password);
    }

    // passwords are stored as SHA-256, never as plain text

    private String hash(String text) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getPharmacyName() {

        String name = properties.getProperty("pharmacyName", DEFAULT_PHARMACY_NAME);

        return name.isBlank() ? DEFAULT_PHARMACY_NAME : name;
    }

    public String getAddress() {
        return properties.getProperty("address", "");
    }

    public String getPhone() {
        return properties.getProperty("phone", "");
    }

    // text under the pharmacy name: "address • phone", or the default title

    public String getSubtitle() {

        String address = getAddress();
        String phone = getPhone();

        if (address.isBlank() && phone.isBlank()) {
            return DEFAULT_SUBTITLE;
        }

        if (address.isBlank()) {
            return phone;
        }

        if (phone.isBlank()) {
            return address;
        }

        return address + "  •  " + phone;
    }

    // the name typed in Settings (empty = use the login name)

    public String getUserName() {
        return properties.getProperty("userName", "");
    }

    // name shown in the header: Settings name if set, otherwise the login name

    public String getDisplayName(String loginName) {

        String name = getUserName();

        return name.isBlank() ? loginName : name;
    }

    public String getCurrency() {
        return properties.getProperty("currency", DEFAULT_CURRENCY);
    }

    // 12.5 -> "12.50 EGP"

    public String formatMoney(double amount) {

        String number = String.format(Locale.US, "%.2f", amount);

        String currency = getCurrency();

        return currency.isBlank() ? number : number + " " + currency;
    }

    public int getLowStockLimit() {

        try {
            int limit = Integer.parseInt(
                    properties.getProperty("lowStockLimit", String.valueOf(DEFAULT_LOW_STOCK_LIMIT)).trim()
            );

            return Math.max(limit, 0);

        } catch (NumberFormatException e) {
            return DEFAULT_LOW_STOCK_LIMIT;
        }
    }

    public boolean isConfirmSale() {
        return Boolean.parseBoolean(properties.getProperty("confirmSale", "true"));
    }
}
