package com.musicplayer.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.musicplayer.model.Song;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class PlaylistController {

    private Stage stage;
    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private ObservableList<Song> filteredSongs = FXCollections.observableArrayList();
    private Song currentSong;
    private int currentIndex = 0;
    private boolean isPlaying = false;
    
    // UI Components
    private Label nowPlayingTitle;
    private Label nowPlayingArtist;
    private Label playBtn;
    private ProgressBar seekBar;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private VBox songListContainer;
    private Label sectionTitleLabel;
    private Label sectionSubtitleLabel;
    private Button addSongBtn;

    // Playlist & Media
    private VBox customPlaylistContainer;
    private Map<String, ObservableList<Song>> playlistMap = new HashMap<>();
    private ComboBox<String> playlistSelector = new ComboBox<>();
    private MediaPlayer mediaPlayer;

    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;
        
        loadSongsFromSupabase();
        
        BorderPane root = buildMainUI();
        
        Scene scene = new Scene(root, 1280, 680);
        scene.setFill(Color.TRANSPARENT);
        
        stage.setTitle("🎵 Melodify - Music Player");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        
        playEntryAnimation(root);
        enableDrag(root, stage);
    }

    // ============================================================
    // SUPABASE DATABASE LOGIC
    // ============================================================
    private void loadSongsFromSupabase() {
        String sql = "SELECT * FROM songs ORDER BY id ASC";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            allSongs.clear();
            while (rs.next()) {
                Song song = new Song(
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("album"),
                    rs.getString("duration"),
                    rs.getString("genre"),
                    rs.getString("cover_emoji")
                );
                song.setFileUrl(rs.getString("file_url"));
                allSongs.add(song);
            }
            filteredSongs.setAll(allSongs);
            
            if (!allSongs.isEmpty()) {
                currentSong = allSongs.get(0);
            }
        } catch (Exception e) {
            System.err.println("Gagal sinkronisasi dengan Supabase Cloud.");
            e.printStackTrace();
        }
    }

    private void saveSongToSupabase(Song song) {
        String sql = "INSERT INTO songs (title, artist, album, duration, genre, cover_emoji, file_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setString(4, song.getDuration());
            pstmt.setString(5, song.getGenre());
            pstmt.setString(6, song.getCoverEmoji());
            pstmt.setString(7, song.getFileUrl());
            pstmt.executeUpdate();

            allSongs.add(song);
            filteredSongs.setAll(allSongs);
            refreshSongList();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setHeaderText(null);
            success.setContentText("Lagu berhasil dikirim ke Supabase!");
            success.show();
        } catch (Exception e) {
            allSongs.add(song);
            filteredSongs.setAll(allSongs);
            refreshSongList();
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Gagal simpan ke cloud, lagu disimpan lokal sementara.\nError: " + e.getMessage());
            alert.show();
        }
    }

    private void deleteSongFromSupabase(Song song) {
        String sql = "DELETE FROM songs WHERE title = ? AND artist = ?";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.executeUpdate();

            allSongs.remove(song);
            filteredSongs.setAll(allSongs);
            
            for (ObservableList<Song> pl : playlistMap.values()) {
                pl.remove(song);
            }
            refreshSongList();

            if (currentSong == song) {
                if (mediaPlayer != null) { 
                    mediaPlayer.stop(); 
                    mediaPlayer.dispose(); 
                    mediaPlayer = null; 
                }
                currentSong = null;
                isPlaying = false;
                nowPlayingTitle.setText("No Song");
                nowPlayingArtist.setText("Unknown Artist");
                playBtn.setText("▶");
                currentTimeLabel.setText("0:00");
                totalTimeLabel.setText("0:00");
                seekBar.setProgress(0);
            }
        } catch (Exception e) {
            allSongs.remove(song);
            filteredSongs.setAll(allSongs);
            refreshSongList();
        }
    }

    // ============================================================
    // MAIN UI BUILDER
    // ============================================================
    private BorderPane buildMainUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0d1a; -fx-background-radius: 20;");
        
        root.setTop(buildTitleBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildPlaylistSection());
        root.setRight(buildNowPlayingPanel());
        root.setBottom(buildPlayerControls());
        
        return root;
    }

    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(15, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        
        Label logo = new Label("🎵 MELODIFY");
        logo.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        
        DropShadow logoGlow = new DropShadow();
        logoGlow.setRadius(15);
        logoGlow.setColor(Color.web("#a855f7"));
        logo.setEffect(logoGlow);

        Region spacer = new Region(); 
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label min = createWindowBtn("—", "#f59e0b");
        Label max = createWindowBtn("⬜", "#22c55e");
        Label close = createWindowBtn("✕", "#ef4444");

        min.setOnMouseClicked(e -> stage.setIconified(true));
        max.setOnMouseClicked(e -> stage.setMaximized(!stage.isMaximized()));
        close.setOnMouseClicked(e -> {
            if (mediaPlayer != null) mediaPlayer.stop();
            stage.close();
        });

        HBox controls = new HBox(8);
        controls.getChildren().addAll(min, max, close);
        
        bar.getChildren().addAll(logo, spacer, controls);
        return bar;
    }

    private Label createWindowBtn(String text, String color) {
        Label btn = new Label(text);
        btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8; -fx-cursor: hand;"));
        return btn;
    }

    // ============================================================
    // SIDEBAR & PLAYLISTS
    // ============================================================
    private VBox buildSidebar() {
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

        customPlaylistContainer = new VBox(5);
        sidebar.getChildren().addAll(sep, pHeaderBox, customPlaylistContainer);
        
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
            if (!pName.isEmpty() && !playlistMap.containsKey(pName)) {
                playlistMap.put(pName, FXCollections.observableArrayList());
                playlistSelector.getItems().add(pName);
                customPlaylistContainer.getChildren().add(createPlaylistSidebarItem("🎵", pName, "0 songs"));
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
        indicator.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#a855f7")), new Stop(1, Color.web("#06b6d4"))));
        indicator.setVisible(isInitiallyActive);
        
        if (isInitiallyActive) {
            item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
        }
        
        item.getChildren().addAll(indicator, iconLabel, textLabel);

        item.setOnMouseClicked(e -> {
            switch (text) {
                case "Home":
                    sectionTitleLabel.setText("Home"); 
                    sectionSubtitleLabel.setText("Welcome back! Ready to listen to some music?");
                    addSongBtn.setVisible(true); 
                    filteredSongs.setAll(allSongs); 
                    break;
                case "Discover":
                    sectionTitleLabel.setText("Discover"); 
                    sectionSubtitleLabel.setText("Rekomendasi hits viral buat lu hari ini 🔥");
                    addSongBtn.setVisible(false); 
                    
                    // JURUS MAGIC: Ngacak urutan lagu biar berasa kayak dikasih "Rekomendasi" baru
                    filteredSongs.clear();
                    filteredSongs.addAll(allSongs);
                    java.util.Collections.shuffle(filteredSongs); 
                    
                    break;
                case "Liked Songs":
                    sectionTitleLabel.setText("Liked Songs"); 
                    sectionSubtitleLabel.setText("Your absolute favorites 💖");
                    addSongBtn.setVisible(false); 
                    filteredSongs.clear(); 
                    allSongs.stream().filter(Song::isLiked).forEach(filteredSongs::add); 
                    break;
            }
            refreshSongList();
            
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
            playlistMap.remove(name); 
            playlistSelector.getItems().remove(name);
            customPlaylistContainer.getChildren().remove(item);
            if (sectionTitleLabel.getText().equals(name)) { 
                sectionTitleLabel.setText("Home"); 
                filteredSongs.setAll(allSongs); 
                refreshSongList(); 
            }
        });
        item.setOnMouseClicked(e -> {
            sectionTitleLabel.setText(name); 
            sectionSubtitleLabel.setText("Custom User Playlist Collection");
            filteredSongs.clear(); 
            ObservableList<Song> saved = playlistMap.get(name);
            if (saved != null) {
                filteredSongs.addAll(saved);
            }
            refreshSongList();
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

    // ============================================================
    // CENTER PLAYLIST UI
    // ============================================================
    private VBox buildPlaylistSection() {
        VBox container = new VBox(0); 
        container.setStyle("-fx-background-color: #0d0d1a;");
        
        HBox header = new HBox(15); 
        header.setPadding(new Insets(10, 25, 15, 25)); 
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox tBox = new VBox(4);
        sectionTitleLabel = new Label("Home");
        sectionSubtitleLabel = new Label("Welcome back! Ready to listen to some music?");
        sectionTitleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: white;");
        sectionSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");
        tBox.getChildren().addAll(sectionTitleLabel, sectionSubtitleLabel);

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

        addSongBtn = new Button("+ Add Song");
        addSongBtn.setStyle("-fx-background-color: linear-gradient(to right, #a855f7, #06b6d4); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 10 20; -fx-cursor: hand; -fx-border-color: transparent;");
        addSongBtn.setOnAction(e -> openAddSongDialog());
        
        header.getChildren().addAll(tBox, spacer, sBox, addSongBtn);

        ScrollPane sp = new ScrollPane(); 
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;"); 
        sp.setFitToWidth(true); 
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        songListContainer = new VBox(2); 
        songListContainer.setPadding(new Insets(5, 20, 20, 20));
        sp.setContent(songListContainer);

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

        // Gambar UI lagu pertama kali saat di-load
        refreshSongList();

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredSongs.clear();
            if (newV == null || newV.isEmpty()) {
                if (sectionTitleLabel.getText().equals("Liked Songs")) {
                    allSongs.stream().filter(Song::isLiked).forEach(filteredSongs::add);
                } else if (playlistMap.containsKey(sectionTitleLabel.getText())) {
                    filteredSongs.addAll(playlistMap.get(sectionTitleLabel.getText()));
                } else {
                    filteredSongs.addAll(allSongs);
                }
            } else {
                String low = newV.toLowerCase();
                allSongs.stream().filter(s -> s.getTitle().toLowerCase().contains(low) || s.getArtist().toLowerCase().contains(low)).forEach(filteredSongs::add);
            }
            refreshSongList();
        });

        container.getChildren().addAll(header, colHeaders, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return container;
    }

    private void refreshSongList() {
        songListContainer.getChildren().clear();
        for (int i = 0; i < filteredSongs.size(); i++) {
            songListContainer.getChildren().add(createSongRow(filteredSongs.get(i), i + 1));
        }
    }

    private HBox createSongRow(Song song, int number) {
        HBox row = new HBox(); 
        row.setAlignment(Pos.CENTER_LEFT); 
        row.setPadding(new Insets(10, 5, 10, 5)); 
        row.setCursor(javafx.scene.Cursor.HAND); 
        row.setStyle("-fx-background-radius: 12;");
        
        boolean isCurrent = (song == currentSong);
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
                if (r == ButtonType.OK) deleteSongFromSupabase(song); 
            });
        });
        
        acts.getChildren().addAll(lBtn, del);

        row.getChildren().addAll(numP, tSec, alb, gen, dur, acts);
        row.setOnMouseEntered(e -> { 
            if (!isCurrent) row.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;"); 
            del.setVisible(true); 
        });
        row.setOnMouseExited(e -> { 
            if (!isCurrent) row.setStyle("-fx-background-color: transparent;"); 
            del.setVisible(false); 
        });
        row.setOnMouseClicked(e -> playSong(song));
        return row;
    }

    // ============================================================
    // NOW PLAYING & RIGHT PANEL
    // ============================================================
    private VBox buildNowPlayingPanel() {
        VBox panel = new VBox(10); 
        panel.setPrefWidth(260); 
        panel.setPadding(new Insets(10, 15, 10, 15)); 
        panel.setStyle("-fx-background-color: #111127; -fx-border-color: transparent transparent transparent #1e1e3a; -fx-border-width: 0 0 0 1;");
        
        Label pTitle = new Label("NOW PLAYING"); 
        pTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        nowPlayingTitle = new Label(currentSong != null ? currentSong.getTitle() : "No Song"); 
        nowPlayingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");
        
        nowPlayingArtist = new Label(currentSong != null ? currentSong.getArtist() : "Unknown Artist"); 
        nowPlayingArtist.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");

        panel.getChildren().addAll(pTitle, createAlbumArt(), nowPlayingTitle, nowPlayingArtist, buildProgressSection(), buildVolumeSection(), buildAddToPlaylistSection());
        return panel;
    }

    private StackPane createAlbumArt() {
        Rectangle artBg = new Rectangle(170, 170); 
        artBg.setArcWidth(25); 
        artBg.setArcHeight(25);
        artBg.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#a855f7")), new Stop(1, Color.web("#06b6d4"))));
        
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

    private VBox buildProgressSection() {
        VBox sec = new VBox(8); 
        HBox tRow = new HBox();
        
        currentTimeLabel = new Label("0:00"); 
        currentTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
        
        totalTimeLabel = new Label(currentSong != null ? currentSong.getDuration() : "0:00"); 
        totalTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
        
        Region sp = new Region(); 
        HBox.setHgrow(sp, Priority.ALWAYS); 
        
        tRow.getChildren().addAll(currentTimeLabel, sp, totalTimeLabel);
        
        seekBar = new ProgressBar(0);
        seekBar.setMaxWidth(Double.MAX_VALUE); 
        seekBar.setStyle("-fx-accent: #a855f7; -fx-pref-height: 4px;");
        
        sec.getChildren().addAll(tRow, seekBar);
        return sec;
    }

    private HBox buildVolumeSection() {
        Slider vSlider = new Slider(0, 1.0, 0.7); 
        HBox.setHgrow(vSlider, Priority.ALWAYS);
        vSlider.valueProperty().addListener((obs, old, val) -> { 
            if (mediaPlayer != null) mediaPlayer.setVolume(val.doubleValue()); 
        });
        
        HBox sec = new HBox(10);
        sec.setAlignment(Pos.CENTER_LEFT);
        sec.getChildren().addAll(new Label("🔊"), vSlider);
        return sec;
    }

    private VBox buildAddToPlaylistSection() {
        VBox sec = new VBox(8); 
        sec.setPadding(new Insets(15, 0, 0, 0));
        
        Label lbl = new Label("ADD CURRENT SONG TO:"); 
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");
        
        playlistSelector.setPromptText("Choose playlist..."); 
        playlistSelector.setMaxWidth(Double.MAX_VALUE);
        playlistSelector.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 5;");
        playlistSelector.setCellFactory(lv -> new ListCell<>() { 
            @Override protected void updateItem(String i, boolean e) { 
                super.updateItem(i, e); 
                setText(e ? null : i); 
                setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white;"); 
            } 
        });

        Button addB = new Button("➕ Add to Selected Playlist");
        addB.setMaxWidth(Double.MAX_VALUE); 
        addB.setStyle("-fx-background-color: #ffffff10; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 12; -fx-padding: 8; -fx-cursor: hand;");
        
        addB.setOnAction(e -> {
            String sel = playlistSelector.getValue();
            if (sel == null) { 
                Alert w = new Alert(Alert.AlertType.WARNING);
                w.setHeaderText(null);
                w.setContentText("Pilih playlist kustom dulu bre!");
                w.show(); 
                return; 
            }
            if (currentSong != null) {
                ObservableList<Song> sList = playlistMap.get(sel);
                if (sList != null && !sList.contains(currentSong)) {
                    sList.add(currentSong);
                    for (Node n : customPlaylistContainer.getChildren()) {
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
        
        sec.getChildren().addAll(lbl, playlistSelector, addB);
        return sec;
    }

    // ============================================================
    // BOTTOM PLAYER CONTROLS
    // ============================================================
    private HBox buildPlayerControls() {
        HBox controls = new HBox(30); 
        controls.setPadding(new Insets(15, 30, 20, 30)); 
        controls.setAlignment(Pos.CENTER); 
        controls.setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3a transparent transparent transparent;");
        
        Label prev = new Label("⏮"); 
        prev.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");
        
        playBtn = new Label("▶"); 
        playBtn.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #a855f7; -fx-background-radius: 50; -fx-padding: 10 15; -fx-cursor: hand;");
        
        Label next = new Label("⏭"); 
        next.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");

        playBtn.setOnMouseClicked(e -> togglePlay());
        prev.setOnMouseClicked(e -> handlePrev());
        next.setOnMouseClicked(e -> handleNext());
        
        controls.getChildren().addAll(prev, playBtn, next);
        return controls;
    }

private void playSong(Song song) {
        if (mediaPlayer != null) { 
            mediaPlayer.stop(); 
            mediaPlayer.dispose(); 
        }
        currentSong = song;
        currentIndex = allSongs.indexOf(song);
        isPlaying = true;
        nowPlayingTitle.setText(song.getTitle());
        nowPlayingArtist.setText(song.getArtist());
        playBtn.setText("⏸");
        refreshSongList();

        try {
            String audioUrl = song.getFileUrl();
            if (audioUrl == null || audioUrl.isEmpty()) {
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"; 
            }
            
            // Jaga-jaga kalau link dari Supabase ada spasinya biar Java gak error
            audioUrl = audioUrl.replace(" ", "%20");

            Media media = new Media(audioUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.7);
            
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                Platform.runLater(() -> {
                    if (mediaPlayer.getTotalDuration() != null) {
                        double current = newTime.toSeconds(); 
                        double total = mediaPlayer.getTotalDuration().toSeconds();
                        if (total > 0) {
                            seekBar.setProgress(current / total);
                            int min = (int) current / 60;
                            int sec = (int) current % 60;
                            currentTimeLabel.setText(String.format("%d:%02d", min, sec));
                        }
                    }
                });
            });

            // MESIN SINKRONISASI WAKTU
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    // 1. Ambil durasi asli dari file MP3-nya
                    double total = mediaPlayer.getTotalDuration().toSeconds();
                    int totalMin = (int) total / 60;
                    int totalSec = (int) total % 60;
                    String realDuration = String.format("%d:%02d", totalMin, totalSec);
                    
                    // 2. Tampilkan di player kanan bawah
                    totalTimeLabel.setText(realDuration);

                    // 3. JURUS MAGIC: Ubah angka "3:30" bawaan database jadi durasi asli di list layar tengah!
                    if (currentSong != null) {
                        currentSong.setDuration(realDuration);
                        refreshSongList(); 
                    }
                });
            });

            mediaPlayer.play();
            mediaPlayer.setOnEndOfMedia(this::handleNext);
        } catch (Exception e) { 
            System.err.println("Gagal memutar audio: " + e.getMessage()); 
        }
    }

    private void togglePlay() {
        // Kalau database bener-bener kosong, tombolnya ga usah ngapa-ngapain
        if (allSongs.isEmpty()) return;

        // Kalau mesin pemutar belum nyala (karena baru buka aplikasi)
        if (mediaPlayer == null) {
            if (currentSong == null) {
                // Putar lagu urutan pertama
                playSong(allSongs.get(0));
            } else {
                // Putar lagu yang lagi mejeng di panel kanan
                playSong(currentSong);
            }
            return;
        }

        // Kalau mesin udah nyala, tinggal pause atau resume kayak biasa
        isPlaying = !isPlaying;
        if (isPlaying) { 
            mediaPlayer.play(); 
            playBtn.setText("⏸"); 
        } else { 
            mediaPlayer.pause(); 
            playBtn.setText("▶"); 
        }
    }

    private void handleNext() { 
        if (!allSongs.isEmpty()) { 
            currentIndex = (currentIndex + 1) % allSongs.size(); 
            playSong(allSongs.get(currentIndex)); 
        } 
    }
    
    private void handlePrev() { 
        if (!allSongs.isEmpty()) { 
            currentIndex = (currentIndex - 1 + allSongs.size()) % allSongs.size(); 
            playSong(allSongs.get(currentIndex)); 
        } 
    }

private void openAddSongDialog() {
        Dialog<Song> dialog = new Dialog<>(); 
        dialog.setTitle("Tambah Lagu Baru"); 
        dialog.initStyle(StageStyle.UTILITY);
        
        ButtonType saveButtonType = new ButtonType("Save to Supabase", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); 
        grid.setVgap(10); 
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField t = new TextField(); t.setPromptText("Judul");
        TextField a = new TextField(); a.setPromptText("Artis");
        TextField al = new TextField(); al.setPromptText("Album");
        TextField g = new TextField(); g.setPromptText("Genre");
        TextField d = new TextField(); d.setPromptText("Durasi");
        TextField e = new TextField(); e.setPromptText("Emoji");
        TextField u = new TextField(); u.setPromptText("URL MP3 (Link Supabase Storage)");
        
        grid.addRow(0, new Label("Judul:"), t); 
        grid.addRow(1, new Label("Artis:"), a); 
        grid.addRow(2, new Label("Album:"), al);
        grid.addRow(3, new Label("Genre:"), g); 
        grid.addRow(4, new Label("Durasi:"), d); 
        grid.addRow(5, new Label("Emoji:"), e); 
        grid.addRow(6, new Label("URL MP3:"), u); 
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return new Song(t.getText(), a.getText(), al.getText(), d.getText(), g.getText(), e.getText());
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(s -> { 
            // Langsung ambil text dari kolom URL apa adanya
            s.setFileUrl(u.getText().trim()); 
            saveSongToSupabase(s); 
        });
    }

    // ============================================================
    // WINDOW EFFECTS & ANIMATIONS
    // ============================================================
    private double xOffset = 0, yOffset = 0;
    private void enableDrag(Node root, Stage stage) {
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

    private void playEntryAnimation(Node root) {
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