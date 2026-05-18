package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SongListBuilder {

    private final PlaylistController controller;

    public SongListBuilder(PlaylistController controller) {
        this.controller = controller;
    }

    public VBox buildPlaylistSection() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: #0d0d1a;");

        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 25, 15, 25));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox tBox = new VBox(4);
        controller.sectionTitleLabel = new Label("Home");
        controller.sectionSubtitleLabel = new Label("Welcome back! Ready to listen to some music?");
        controller.sectionTitleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: white;");
        controller.sectionSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");
        tBox.getChildren().addAll(controller.sectionTitleLabel, controller.sectionSubtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox sBox = new HBox(8);
        Label searchIcon = new Label("🔍");
        TextField searchField = new TextField();
        sBox.setAlignment(Pos.CENTER);
        sBox.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 25; -fx-padding: 8 15;");
        searchField.setPromptText("Search songs...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: transparent; -fx-pref-width: 180px;");
        sBox.getChildren().addAll(searchIcon, searchField);

        controller.addSongBtn = new javafx.scene.control.Button("+ Add Song");
        controller.addSongBtn.setStyle("-fx-background-color: linear-gradient(to right, #a855f7, #06b6d4); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 10 20; -fx-cursor: hand; -fx-border-color: transparent;");
        controller.addSongBtn.setOnAction(e -> controller.dialogManager.openAddSongDialog());

        header.getChildren().addAll(tBox, spacer, sBox, controller.addSongBtn);

        ScrollPane sp = new ScrollPane();
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        controller.songListContainer = new VBox(2);
        controller.songListContainer.setPadding(new Insets(5, 20, 20, 20));
        sp.setContent(controller.songListContainer);

        HBox colHeaders = new HBox();
        colHeaders.setPadding(new Insets(5, 25, 8, 25));
        colHeaders.setStyle("-fx-border-color: transparent transparent #1e1e3a transparent; -fx-border-width: 0 0 1 0;");

        String[] cNames = {"#", "TITLE", "ALBUM", "GENRE", "TIME", ""};
        double[] widths = {40, 280, 180, 120, 60, 80};
        for (int i = 0; i < cNames.length; i++) {
            Label c = new Label(cNames[i]);
            c.setMinWidth(widths[i]);
            c.setMaxWidth(widths[i]);
            c.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");
            colHeaders.getChildren().add(c);
        }

        refreshSongList();

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            controller.filteredSongs.clear();
            if (newV == null || newV.isEmpty()) {
                if (controller.sectionTitleLabel.getText().equals("Liked Songs")) {
                    controller.allSongs.stream().filter(Song::isLiked).forEach(controller.filteredSongs::add);
                } else if (controller.playlistMap.containsKey(controller.sectionTitleLabel.getText())) {
                    controller.filteredSongs.addAll(controller.playlistMap.get(controller.sectionTitleLabel.getText()));
                } else {
                    controller.filteredSongs.addAll(controller.allSongs);
                }
            } else {
                String low = newV.toLowerCase();
                controller.allSongs.stream()
                        .filter(s -> s.getTitle().toLowerCase().contains(low) || s.getArtist().toLowerCase().contains(low))
                        .forEach(controller.filteredSongs::add);
            }
            refreshSongList();
        });

        container.getChildren().addAll(header, colHeaders, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return container;
    }

    public void refreshSongList() {
        if (controller.songListContainer == null) {
            return;
        }

        controller.songListContainer.getChildren().clear();
        for (int i = 0; i < controller.filteredSongs.size(); i++) {
            controller.songListContainer.getChildren().add(createSongRow(controller.filteredSongs.get(i), i + 1));
        }
    }

    private HBox createSongRow(Song song, int number) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 5, 10, 5));
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setStyle("-fx-background-radius: 12;");

        boolean isCurrent = (song == controller.currentSong);
        if (isCurrent) {
            row.setStyle("-fx-background-color: linear-gradient(to right, #a855f715, #06b6d410); -fx-background-radius: 12;");
        }

        Label num = new Label(isCurrent ? "▶" : String.valueOf(number));
        num.setStyle("-fx-text-fill: " + (isCurrent ? "#a855f7" : "rgba(255,255,255,0.4)") + ";");
        StackPane numP = new StackPane(num);
        numP.setMinWidth(40);

        HBox tSec = new HBox(12);
        tSec.setMinWidth(280);
        tSec.setAlignment(Pos.CENTER_LEFT);

        Rectangle cBg = new Rectangle(42, 42);
        cBg.setArcWidth(10);
        cBg.setArcHeight(10);
        cBg.setFill(Color.web("#a855f7"));

        Label cEm = new Label(song.getCoverEmoji() != null ? song.getCoverEmoji() : "🎵");
        StackPane cov = new StackPane();
        cov.getChildren().addAll(cBg, cEm);

        VBox sInfo = new VBox(3);
        Label titleLbl = new Label(song.getTitle());
        titleLbl.setStyle("-fx-text-fill: " + (isCurrent ? "#a855f7" : "white") + "; -fx-font-weight: bold;");
        Label artistLbl = new Label(song.getArtist());
        artistLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");
        sInfo.getChildren().addAll(titleLbl, artistLbl);

        tSec.getChildren().addAll(cov, sInfo);

        Label alb = new Label(song.getAlbum());
        alb.setMinWidth(180);
        alb.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");

        Label gen = new Label(song.getGenre());
        gen.setMinWidth(120);
        gen.setStyle("-fx-text-fill: #06b6d4; -fx-background-color: #06b6d415; -fx-background-radius: 20; -fx-padding: 3 8; -fx-font-size: 11px;");

        Label dur = new Label(song.getDuration());
        dur.setMinWidth(60);
        dur.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");

        HBox acts = new HBox(12);
        acts.setMinWidth(80);
        acts.setAlignment(Pos.CENTER_RIGHT);

        Label lBtn = new Label(song.isLiked() ? "❤️" : "🤍");
        lBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        lBtn.setOnMouseClicked(e -> {
            e.consume();
            song.setLiked(!song.isLiked());
            lBtn.setText(song.isLiked() ? "❤️" : "🤍");
        });

        Label del = new Label("🗑️");
        del.setStyle("-fx-font-size: 15px; -fx-cursor: hand;");
        del.setVisible(false);
        del.setOnMouseClicked(e -> {
            e.consume();
            Alert c = new Alert(Alert.AlertType.CONFIRMATION);
            c.setHeaderText(null);
            c.setContentText("Hapus '" + song.getTitle() + "' permanen dari cloud?");
            c.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    controller.databaseManager.deleteSongFromSupabase(song);
                }
            });
        });

        acts.getChildren().addAll(lBtn, del);

        row.getChildren().addAll(numP, tSec, alb, gen, dur, acts);
        row.setOnMouseEntered(e -> {
            if (!isCurrent) {
                row.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;");
            }
            del.setVisible(true);
        });
        row.setOnMouseExited(e -> {
            if (!isCurrent) {
                row.setStyle("-fx-background-color: transparent;");
            }
            del.setVisible(false);
        });
        row.setOnMouseClicked(e -> controller.playerManager.playSong(song));
        return row;
    }
}