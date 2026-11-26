package sample;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sample.controller.LoginController;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        LoginController loginController = new LoginController();

        Scene scene = new Scene(
                loginController.getView().getRoot(),
                1000,
                700
        );

        stage.setTitle("Pharmacy Management System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
