package sample.controller;

import javafx.scene.control.Alert;
import sample.service.SettingsService;
import sample.view.HomeView;

public class LoginController {

    private final HomeView view;

    // passwords are checked (and changed) through SettingsService

    private final SettingsService settingsService = new SettingsService();

    public LoginController() {
        this.view = new HomeView();

        initEvents();
    }

    private void initEvents() {
        view.getLoginButton().setOnAction(event -> handleLogin());

        // السماح بالضغط على Enter من حقل الباسورد لتسجيل الدخول
        view.getPasswordField().setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        String username = view.getUsernameField().getText().trim();
        String password = view.getPasswordField().getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            view.getMessageLabel().setText("Please enter both username and password.");
            return;
        }

        // the password may have been changed from Settings since we started

        settingsService.reload();

        if (settingsService.checkLogin(username, password)) {
            view.getMessageLabel().setText("");
            view.getPasswordField().clear();

            MedicineController medicineController = new MedicineController(username);
            if (view.getRoot().getScene() != null) {
                view.getRoot().getScene().setRoot(medicineController.getView().getRoot());
            }
        } else {
            view.getMessageLabel().setText("Invalid username or password.");
            showAlert("Login Failed", "Incorrect username or password.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public HomeView getView() {
        return view;
    }
}