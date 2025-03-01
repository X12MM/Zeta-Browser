package org.example.browser1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Set up undecorated window
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Browser");
        stage.setScene(scene);
        stage.show();
        
        // Center on screen
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch();
    }
}