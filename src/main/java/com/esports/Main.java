package com.esports;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));

        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.setMaximized(true);

        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}