package com.musicplayer.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AnimationManager {

    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void enableDrag(Node root, Stage stage) {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        root.setOnMouseDragged(e -> {
            if (yOffset < 60) {
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    public static void playEntryAnimation(Node root) {
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);

        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(500), root);
        st.setToX(1.0);
        st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
    }
}