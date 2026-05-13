package com.musicplayer;
import com.musicplayer.controller.PlaylistController;
import com.musicplayer.view.SplashScreen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Splash screen dulu bro!
        showSplashThenMain(primaryStage);
    }

    private void showSplashThenMain(Stage primaryStage) {
        SplashScreen splash = new SplashScreen();
        splash.show(() -> {
            Platform.runLater(() -> {
                PlaylistController controller = new PlaylistController();
                controller.initAndShow(primaryStage);
            });
        });
    }

    public static void main(String[] args) {
    // TAMBAHIN BARIS INI BRE:
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    
    launch(args);
}
}
