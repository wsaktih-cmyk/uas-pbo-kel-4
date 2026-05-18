package com.musicplayer.controller;

import java.util.Optional;

import com.musicplayer.model.Song;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class SidebarBuilder {

    private final PlaylistController controller;

    public SidebarBuilder(PlaylistController controller) {
        this.controller = controller;
    }

    public VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(10, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #111127; -fx-border-color: transparent #1e1e3a transparent transparent; -fx-border-width: 0 1 0 0;");

        sidebar.getChildren().add(createSidebarItem("🏠", "Home", true));
        sidebar.getChildren().add(createSidebarItem("🔍", "Discover", false));
        sidebar.getChildren().add(createSidebarItem("💖", "Liked Songs", false));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a;");

        HBox pHeaderBox = new HBox();
        pHeaderBox.setAlignment(Pos.CENTER_LEFT);
        pHeaderBox.setPadding(new Insets(15, 5, 5, 5));

        Label pHeader = new Label("MY PLAYLISTS");
        pHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label addPBtn = new Label("➕");
        addPBtn.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-cursor: hand;");
        addPBtn.setOnMouseClicked(e -> handleCreatePlaylist());

        pHeaderBox.getChildren().addAll(pHeader, sp, addPBtn);

        controller.customPlaylistContainer = new VBox(5);
        sidebar.getChildren().addAll(sep, pHeaderBox, controller.customPlaylistContainer);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, createUserProfile());

        return sidebar;
    }

    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Create New Playlist");
        dialog.setHeaderText("Bikin playlist kustom baru lu bre");
        dialog.setContentText("Masukkan nama playlist:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String pName = name.trim();
            if (!pName.isEmpty() && !controller.playlistMap.containsKey(pName)) {
                controller.playlistMap.put(pName, FXCollections.observableArrayList());
                controller.playlistSelector.getItems().add(pName);
                controller.customPlaylistContainer.getChildren().add(createPlaylistSidebarItem("🎵", pName, "0 songs"));
            }
        });
    }

    private HBox createSidebarItem(String icon, String text, boolean isInitiallyActive) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (isInitiallyActive ? "bold" : "normal") + "; -fx-text-fill: " + (isInitiallyActive ? "white" : "rgba(255,255,255,0.6)") + ";");

        Rectangle indicator = new Rectangle(3, 20);
        indicator.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a855f7")),
                new Stop(1, Color.web("#06b6d4"))));
        indicator.setVisible(isInitiallyActive);

        if (isInitiallyActive) {
            item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
        }

        item.getChildren().addAll(indicator, iconLabel, textLabel);

        item.setOnMouseClicked(e -> {
            switch (text) {
                case "Home":
                    controller.sectionTitleLabel.setText("Home");
                    controller.sectionSubtitleLabel.setText("Welcome back! Ready to listen to some music?");
                    controller.addSongBtn.setVisible(true);
                    controller.filteredSongs.setAll(controller.allSongs);
                    break;
                case "Discover":
                    controller.sectionTitleLabel.setText("Discover");
                    controller.sectionSubtitleLabel.setText("Rekomendasi hits viral buat lu hari ini 🔥");
                    controller.addSongBtn.setVisible(false);
                    controller.filteredSongs.clear();
                    controller.filteredSongs.addAll(controller.allSongs);
                    java.util.Collections.shuffle(controller.filteredSongs);
                    break;
                case "Liked Songs":
                    controller.sectionTitleLabel.setText("Liked Songs");
                    controller.sectionSubtitleLabel.setText("Your absolute favorites 💖");
                    controller.addSongBtn.setVisible(false);
                    controller.filteredSongs.clear();
                    controller.allSongs.stream().filter(Song::isLiked).forEach(controller.filteredSongs::add);
                    break;
            }
            controller.songListBuilder.refreshSongList();

            VBox parent = (VBox) item.getParent();
            for (Node node : parent.getChildren()) {
                if (node instanceof HBox) {
                    HBox sibling = (HBox) node;
                    if (!sibling.getChildren().isEmpty() && sibling.getChildren().get(0) instanceof Rectangle) {
                        Rectangle ind = (Rectangle) sibling.getChildren().get(0);
                        Label lbl = (Label) sibling.getChildren().get(2);
                        ind.setVisible(false);
                        sibling.setStyle("-fx-background-color: transparent;");
                        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: rgba(255,255,255,0.6);");
                    }
                }
            }

            indicator.setVisible(true);
            item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
            textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        });

        return item;
    }

    private HBox createPlaylistSidebarItem(String emoji, String name, String count) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(8, 15, 8, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);

        Label em = new Label(emoji);
        em.setStyle("-fx-font-size: 16px; -fx-background-color: #ffffff15; -fx-background-radius: 8; -fx-padding: 6 8;");

        VBox info = new VBox(2);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.8);");
        Label countLbl = new Label(count);
        countLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.4);");
        info.getChildren().addAll(nameLbl, countLbl);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label del = new Label("🗑️");
        del.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
        del.setVisible(false);

        item.getChildren().addAll(em, info, sp, del);

        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 10;");
            del.setVisible(true);
        });
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: transparent;");
            del.setVisible(false);
        });

        del.setOnMouseClicked(e -> {
            e.consume();
            controller.playlistMap.remove(name);
            controller.playlistSelector.getItems().remove(name);
            controller.customPlaylistContainer.getChildren().remove(item);

            if (controller.sectionTitleLabel.getText().equals(name)) {
                controller.sectionTitleLabel.setText("Home");
                controller.filteredSongs.setAll(controller.allSongs);
                controller.songListBuilder.refreshSongList();
            }
        });

        item.setOnMouseClicked(e -> {
            controller.sectionTitleLabel.setText(name);
            controller.sectionSubtitleLabel.setText("Custom User Playlist Collection");
            controller.filteredSongs.clear();
            ObservableList<Song> saved = controller.playlistMap.get(name);
            if (saved != null) {
                controller.filteredSongs.addAll(saved);
            }
            controller.songListBuilder.refreshSongList();
        });

        return item;
    }

    private HBox createUserProfile() {
        HBox profile = new HBox(10);
        profile.setPadding(new Insets(12, 15, 12, 15));
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;");

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 24px; -fx-background-color: linear-gradient(to bottom right, #a855f7, #06b6d4); -fx-background-radius: 50; -fx-padding: 5 8;");

        VBox info = new VBox(2);
        Label nameLbl = new Label("Wishang Sakti");
        nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label planLbl = new Label("Premium ✓");
        planLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #a855f7;");
        info.getChildren().addAll(nameLbl, planLbl);

        profile.getChildren().addAll(avatar, info);
        return profile;
    }
}