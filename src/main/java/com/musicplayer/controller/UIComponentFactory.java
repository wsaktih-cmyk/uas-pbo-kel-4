package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;

public class UIComponentFactory {

    private final PlaylistController controller;

    public UIComponentFactory(PlaylistController controller) {
        this.controller = controller;
    }

    public Label createWindowBtn(String text, String color) {
        Label btn = new Label(text);
        btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;"));
        return btn;
    }

    public StackPane createAlbumArt() {
        Rectangle artBg = new Rectangle(170, 170);
        artBg.setArcWidth(25);
        artBg.setArcHeight(25);
        artBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a855f7")),
                new Stop(1, Color.web("#06b6d4"))));

        DropShadow ds = new DropShadow();
        ds.setRadius(20);
        ds.setColor(Color.web("#a855f760"));
        artBg.setEffect(ds);

        Label emoji = new Label("🎵");
        emoji.setStyle("-fx-font-size: 60px;");

        StackPane artPane = new StackPane();
        artPane.getChildren().addAll(artBg, emoji);
        return artPane;
    }

    public VBox buildProgressSection() {
        VBox sec = new VBox(8);
        HBox tRow = new HBox();

        controller.currentTimeLabel = new Label("0:00");
        controller.currentTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");

        controller.totalTimeLabel = new Label(controller.currentSong != null ? controller.currentSong.getDuration() : "0:00");
        controller.totalTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        tRow.getChildren().addAll(controller.currentTimeLabel, sp, controller.totalTimeLabel);

        controller.seekBar = new ProgressBar(0);
        controller.seekBar.setMaxWidth(Double.MAX_VALUE);
        controller.seekBar.setStyle("-fx-accent: #a855f7; -fx-pref-height: 4px;");

        sec.getChildren().addAll(tRow, controller.seekBar);
        return sec;
    }

    public HBox buildVolumeSection() {
        Slider vSlider = new Slider(0, 1.0, 0.7);
        HBox.setHgrow(vSlider, Priority.ALWAYS);
        vSlider.valueProperty().addListener((obs, old, val) -> {
            if (controller.mediaPlayer != null) {
                controller.mediaPlayer.setVolume(val.doubleValue());
            }
        });

        HBox sec = new HBox(10);
        sec.setAlignment(Pos.CENTER_LEFT);
        sec.getChildren().addAll(new Label("🔊"), vSlider);
        return sec;
    }

    public VBox buildAddToPlaylistSection() {
        VBox sec = new VBox(8);
        sec.setPadding(new Insets(15, 0, 0, 0));

        Label lbl = new Label("ADD CURRENT SONG TO:");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        controller.playlistSelector.setPromptText("Choose playlist...");
        controller.playlistSelector.setMaxWidth(Double.MAX_VALUE);
        controller.playlistSelector.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 5;");
        controller.playlistSelector.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(e ? null : i);
                setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white;");
            }
        });

        Button addB = new Button("➕ Add to Selected Playlist");
        addB.setMaxWidth(Double.MAX_VALUE);
        addB.setStyle("-fx-background-color: #ffffff10; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 12; -fx-padding: 8; -fx-cursor: hand;");

        addB.setOnAction(e -> {
            String sel = controller.playlistSelector.getValue();
            if (sel == null) {
                Alert w = new Alert(Alert.AlertType.WARNING);
                w.setHeaderText(null);
                w.setContentText("Pilih playlist kustom dulu bre!");
                w.show();
                return;
            }
            if (controller.currentSong != null) {
                ObservableList<Song> sList = controller.playlistMap.get(sel);
                if (sList != null && !sList.contains(controller.currentSong)) {
                    sList.add(controller.currentSong);

                    for (Node n : controller.customPlaylistContainer.getChildren()) {
                        if (n instanceof HBox) {
                            HBox row = (HBox) n;
                            VBox infoBox = (VBox) row.getChildren().get(1);
                            Label nameLbl = (Label) infoBox.getChildren().get(0);
                            Label countLbl = (Label) infoBox.getChildren().get(1);

                            if (nameLbl.getText().equals(sel)) {
                                countLbl.setText(sList.size() + " songs");
                                break;
                            }
                        }
                    }

                    Alert s = new Alert(Alert.AlertType.INFORMATION);
                    s.setHeaderText(null);
                    s.setContentText("Berhasil ditambahkan!");
                    s.show();
                }
            }
        });

        sec.getChildren().addAll(lbl, controller.playlistSelector, addB);
        return sec;
    }

    public HBox buildPlayerControls() {
        HBox controls = new HBox(30);
        controls.setPadding(new Insets(15, 30, 20, 30));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3a transparent transparent transparent;");

        Label prev = new Label("⏮");
        prev.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");

        controller.playBtn = new Label("▶");
        controller.playBtn.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #a855f7; -fx-background-radius: 50; -fx-padding: 10 15; -fx-cursor: hand;");

        Label next = new Label("⏭");
        next.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");

        controller.playBtn.setOnMouseClicked(e -> controller.playerManager.togglePlay());
        prev.setOnMouseClicked(e -> controller.playerManager.handlePrev());
        next.setOnMouseClicked(e -> controller.playerManager.handleNext());

        controls.getChildren().addAll(prev, controller.playBtn, next);
        return controls;
    }
}