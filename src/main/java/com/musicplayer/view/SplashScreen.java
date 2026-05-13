package com.musicplayer.view;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SplashScreen {

    private Stage splashStage;

    public void show(Runnable onFinished) {
        splashStage = new Stage();
        // FIX: Ubah ke TRANSPARENT biar nggak kena bug white screen di Windows
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashStage.setAlwaysOnTop(true);

        // Root container
        StackPane root = new StackPane();
        // FIX: Background langsung dipasang di Root pakai CSS biar stabil
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0a0a1a, #1a0a2e, #0a1628);" +
            "-fx-background-radius: 30;"
        );

        // Particle circles (background decoration)
        Pane particlePane = createParticles();

        // Center content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);

        // Music icon animated
        Label musicIcon = new Label("🎵");
        musicIcon.setStyle("-fx-font-size: 80px;");
        musicIcon.setEffect(new Glow(0.8));

        // App title
        Label titleLabel = new Label("MELODIFY");
        
        // Gradient text effect via CSS
        titleLabel.setStyle(
            "-fx-font-size: 42px;" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );
        
        DropShadow titleGlow = new DropShadow();
        titleGlow.setColor(Color.web("#a855f7"));
        titleGlow.setRadius(20);
        titleGlow.setSpread(0.3);
        titleLabel.setEffect(titleGlow);

        // Subtitle
        Label subLabel = new Label("Your Ultimate Music Experience");
        subLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: rgba(255,255,255,0.6);" +
            "-fx-font-family: 'Segoe UI';"
        );

        // Loading bar container
        StackPane progressContainer = new StackPane();
        progressContainer.setMaxWidth(300);

        Rectangle progressBg = new Rectangle(300, 4);
        progressBg.setArcWidth(4);
        progressBg.setArcHeight(4);
        progressBg.setFill(Color.web("#ffffff20"));

        Rectangle progressBar = new Rectangle(0, 4);
        progressBar.setArcWidth(4);
        progressBar.setArcHeight(4);
        progressBar.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#a855f7")),
            new Stop(1, Color.web("#06b6d4"))
        ));
        
        DropShadow barGlow = new DropShadow();
        barGlow.setColor(Color.web("#a855f7"));
        barGlow.setRadius(8);
        progressBar.setEffect(barGlow);

        progressContainer.getChildren().addAll(progressBg, progressBar);
        StackPane.setAlignment(progressBar, javafx.geometry.Pos.CENTER_LEFT);

        // Loading text
        Label loadingText = new Label("Initializing...");
        loadingText.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: rgba(255,255,255,0.4);" +
            "-fx-font-family: 'Segoe UI';"
        );

        content.getChildren().addAll(musicIcon, titleLabel, subLabel,
                                    progressContainer, loadingText);

        // FIX: Rectangle background dihapus, sisa partikel dan konten
        root.getChildren().addAll(particlePane, content);

        Scene scene = new Scene(root, 500, 400);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.show();

        // Center stage
        splashStage.centerOnScreen();

        // ===== ANIMATIONS =====

        // 1. Icon bounce animation
        ScaleTransition iconBounce = new ScaleTransition(Duration.millis(800), musicIcon);
        iconBounce.setFromX(0);
        iconBounce.setFromY(0);
        iconBounce.setToX(1);
        iconBounce.setToY(1);
        iconBounce.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        // 2. Title fade + slide up
        content.setTranslateY(30);
        content.setOpacity(0); // Transparansi dinyalain lagi biar efek FadeIn jalan
        
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(600), content);
        slideUp.setFromY(30);
        slideUp.setToY(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition titleAnim = new ParallelTransition(slideUp, fadeIn);
        titleAnim.setDelay(Duration.millis(200));

        // 3. Progress bar animation
        Timeline progressAnim = new Timeline();
        String[] loadingMessages = {
            "Loading music library...",
            "Syncing playlists...",
            "Applying cool effects...",
            "Almost ready..."
        };
        
        for (int i = 0; i <= 100; i++) {
            final int progress = i;
            KeyFrame kf = new KeyFrame(Duration.millis(i * 22), e -> {
                progressBar.setWidth(progress * 3.0);
                int msgIndex = Math.min(progress / 25, loadingMessages.length - 1);
                loadingText.setText(loadingMessages[msgIndex]);
            });
            progressAnim.getKeyFrames().add(kf);
        }

        // 4. Icon continuous glow pulse
        Timeline iconPulse = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(musicIcon.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(800),
                new KeyValue(musicIcon.opacityProperty(), 0.5, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(1600),
                new KeyValue(musicIcon.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        iconPulse.setCycleCount(Animation.INDEFINITE);

        // 5. Rotate icon slowly
        RotateTransition iconRotate = new RotateTransition(Duration.seconds(3), musicIcon);
        iconRotate.setByAngle(360);
        iconRotate.setCycleCount(Animation.INDEFINITE);
        iconRotate.setInterpolator(Interpolator.LINEAR);

        // 6. Fade out & close
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.millis(2600));
        fadeOut.setOnFinished(e -> {
            splashStage.close();
            onFinished.run();
        });

        // Play all
        iconBounce.play();
        titleAnim.play();
        iconPulse.play();
        
        PauseTransition startProgress = new PauseTransition(Duration.millis(400));
        startProgress.setOnFinished(e -> progressAnim.play());
        startProgress.play();
        
        fadeOut.play();
    }

    private Pane createParticles() {
        Pane pane = new Pane();
        pane.setPrefSize(500, 400);
        pane.setMouseTransparent(true);

        String[] notes = {"♪", "♫", "♬", "♩", "🎵", "🎶"};

        for (int i = 0; i < 15; i++) {
            Label particle = new Label(notes[(int)(Math.random() * notes.length)]);
            double size = 10 + Math.random() * 20;
            particle.setStyle(
                "-fx-font-size: " + size + "px;" +
                "-fx-text-fill: rgba(168,85,247," + (0.1 + Math.random() * 0.3) + ");"
            );
            particle.setLayoutX(Math.random() * 480);
            particle.setLayoutY(Math.random() * 380);

            // Float animation
            TranslateTransition float1 = new TranslateTransition(
                Duration.seconds(3 + Math.random() * 4), particle);
            float1.setByY(-(20 + Math.random() * 40));
            float1.setByX((Math.random() - 0.5) * 30);
            float1.setCycleCount(Animation.INDEFINITE);
            float1.setAutoReverse(true);
            float1.setDelay(Duration.millis(Math.random() * 2000));
            float1.play();

            FadeTransition particleFade = new FadeTransition(
                Duration.seconds(2 + Math.random() * 3), particle);
            particleFade.setFromValue(0.1);
            particleFade.setToValue(0.6);
            particleFade.setCycleCount(Animation.INDEFINITE);
            particleFade.setAutoReverse(true);
            particleFade.play();

            pane.getChildren().add(particle);
        }
        return pane;
    }
}