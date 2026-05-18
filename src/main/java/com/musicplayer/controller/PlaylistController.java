package com.musicplayer.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.musicplayer.model.Song;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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
    private ObservableList<Song> allSongs;
    private ObservableList<Song> filteredSongs;
    private Song currentSong;
    private int currentIndex = 0;
    private boolean isPlaying = false;
    
    // UI Variables
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

    // Custom Playlist & Media Variables
    private VBox customPlaylistContainer;
    private Map<String, ObservableList<Song>> playlistMap = new HashMap<>();
    private ComboBox<String> playlistSelector = new ComboBox<>();
    private int playlistCounter = 1;
    private MediaPlayer mediaPlayer;

    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;
        
        initFallbackLocalData();
        
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

    private void initFallbackLocalData() {
        allSongs = FXCollections.observableArrayList(
            new Song("Blinding Lights", "The Weeknd", "After Hours", "3:20", "Synth-pop", "🌃"),
            new Song("As It Was", "Harry Styles", "Harry's House", "2:37", "Indie Pop", "🏠"),
            new Song("Stay", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3", "2:21", "Pop", "💫")
        );
        
        for (Song s : allSongs) {
            s.setFileUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
        }

        filteredSongs = FXCollections.observableArrayList(allSongs);
        if (!allSongs.isEmpty()) {
            currentSong = allSongs.get(0);
        }
    }

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

    // ============================================================
    // TITLE BAR & WINDOW CONTROLS
    // ============================================================
    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(15, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: transparent;");

        Label logo = new Label("🎵 MELODIFY");
        logo.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        
        DropShadow logoGlow = new DropShadow();
        logoGlow.setColor(Color.web("#a855f7"));
        logoGlow.setRadius(15);
        logo.setEffect(logoGlow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label minimize = createWindowBtn("—", "#f59e0b");
        Label maximize = createWindowBtn("⬜", "#22c55e");
        Label close = createWindowBtn("✕", "#ef4444");

        minimize.setOnMouseClicked(e -> stage.setIconified(true));
        maximize.setOnMouseClicked(e -> stage.setMaximized(!stage.isMaximized()));
        close.setOnMouseClicked(e -> {
            if (mediaPlayer != null) mediaPlayer.stop();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(ev -> stage.close());
            ft.play();
        });

        HBox windowControls = new HBox(8, minimize, maximize, close);
        windowControls.setAlignment(Pos.CENTER);
        bar.getChildren().addAll(logo, spacer, windowControls);
        return bar;
    }

    private Label createWindowBtn(String text, String color) {
        Label btn = new Label(text);
        btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-background-radius: 50; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
        return btn;
    }

    // ============================================================
    // SIDEBAR & PLAYLIST MANAGEMENT
    // ============================================================
    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(10, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #111127; -fx-border-color: transparent #1e1e3a transparent transparent; -fx-border-width: 0 1 0 0;");

        String[][] menuItems = {
            {"🏠", "Home"}, {"🔍", "Discover"}, {"💖", "Liked Songs"}, {"📥", "Downloads"}
        };

        for (int i = 0; i < menuItems.length; i++) {
            HBox item = createSidebarItem(menuItems[i][0], menuItems[i][1], i == 0);
            sidebar.getChildren().add(item);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a;");
        
        HBox playlistHeaderBox = new HBox();
        playlistHeaderBox.setAlignment(Pos.CENTER_LEFT);
        playlistHeaderBox.setPadding(new Insets(15, 5, 5, 5));
        
        Label playlistHeader = new Label("MY PLAYLISTS");
        playlistHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Label addPlaylistBtn = new Label("➕");
        addPlaylistBtn.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-cursor: hand;");
        addPlaylistBtn.setOnMouseClicked(e -> handleCreatePlaylist());
        
        playlistHeaderBox.getChildren().addAll(playlistHeader, headerSpacer, addPlaylistBtn);

        customPlaylistContainer = new VBox(5);
        sidebar.getChildren().addAll(sep, playlistHeaderBox, customPlaylistContainer);
        
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
            String playlistName = name.trim();
            if (!playlistName.isEmpty() && !playlistMap.containsKey(playlistName)) {
                playlistMap.put(playlistName, FXCollections.observableArrayList());
                playlistSelector.getItems().add(playlistName);
                HBox newPlaylistRow = createPlaylistSidebarItem("🎵", playlistName, "0 songs");
                customPlaylistContainer.getChildren().add(newPlaylistRow);
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
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (isInitiallyActive ? "bold" : "normal") + "; -fx-text-fill: " + (isInitiallyActive ? "white" : "rgba(255,255,255,0.6)") + "; -fx-font-family: 'Segoe UI';");

        Rectangle indicator = new Rectangle(3, 20);
        indicator.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#a855f7")), new Stop(1, Color.web("#06b6d4"))));
        indicator.setVisible(isInitiallyActive);

        if (isInitiallyActive) item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
        else item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");

        item.getChildren().addAll(indicator, iconLabel, textLabel);

        item.setOnMouseClicked(e -> {
            switch (text) {
                case "Home":
                    sectionTitleLabel.setText("Home");
                    sectionSubtitleLabel.setText("Welcome back! Ready to listen to some music?");
                    addSongBtn.setVisible(true);
                    filteredSongs.clear(); filteredSongs.addAll(allSongs);
                    break;
                case "Discover":
                    sectionTitleLabel.setText("Discover");
                    sectionSubtitleLabel.setText("Explore new tracks and trending genres");
                    addSongBtn.setVisible(false);
                    filteredSongs.clear(); filteredSongs.addAll(allSongs);
                    break;
                case "Liked Songs":
                    sectionTitleLabel.setText("Liked Songs");
                    sectionSubtitleLabel.setText("Your absolute favorites 💖");
                    addSongBtn.setVisible(false);
                    filteredSongs.clear();
                    allSongs.stream().filter(Song::isLiked).forEach(filteredSongs::add);
                    break;
                case "Downloads":
                    sectionTitleLabel.setText("Downloads");
                    sectionSubtitleLabel.setText("Offline storage mode enabled");
                    addSongBtn.setVisible(false);
                    filteredSongs.clear();
                    break;
            }
            refreshSongList();

            VBox parent = (VBox) item.getParent();
            for (Node node : parent.getChildren()) {
                if (node instanceof HBox && !((HBox)node).getChildren().isEmpty() && ((HBox)node).getChildren().get(0) instanceof Rectangle) {
                    HBox sibling = (HBox) node;
                    Rectangle ind = (Rectangle) sibling.getChildren().get(0);
                    Label lbl = (Label) sibling.getChildren().get(2);
                    ind.setVisible(false);
                    sibling.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                    lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: rgba(255,255,255,0.6);");
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
        item.setStyle("-fx-background-radius: 10;");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 16px; -fx-background-color: #ffffff15; -fx-background-radius: 8; -fx-padding: 6 8 6 8;");

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.8);");
        Label countLabel = new Label(count);
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.4);");
        info.getChildren().addAll(nameLabel, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label deleteBtn = new Label("🗑️");
        deleteBtn.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
        deleteBtn.setVisible(false);
        
        item.getChildren().addAll(emojiLabel, info, spacer, deleteBtn);

        item.setOnMouseEntered(e -> { item.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 10;"); deleteBtn.setVisible(true); });
        item.setOnMouseExited(e -> { item.setStyle("-fx-background-color: transparent; -fx-background-radius: 10;"); deleteBtn.setVisible(false); });

        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Hapus Playlist");
            confirm.setHeaderText(null);
            confirm.setContentText("Yakin mau hapus playlist '" + name + "'?");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    playlistMap.remove(name);
                    playlistSelector.getItems().remove(name);
                    customPlaylistContainer.getChildren().remove(item);
                    
                    if (sectionTitleLabel != null && sectionTitleLabel.getText().equals(name)) {
                        sectionTitleLabel.setText("Home");
                        filteredSongs.clear(); filteredSongs.addAll(allSongs);
                        refreshSongList();
                    }
                }
            });
        });

        item.setOnMouseClicked(e -> {
            if (sectionTitleLabel != null) {
                sectionTitleLabel.setText(name);
                sectionSubtitleLabel.setText("Custom User Playlist");
                addSongBtn.setVisible(true);
                filteredSongs.clear();
                ObservableList<Song> savedSongs = playlistMap.get(name);
                if (savedSongs != null) filteredSongs.addAll(savedSongs);
                refreshSongList();
            }
        });

        return item;
    }

    private HBox createUserProfile() {
        HBox profile = new HBox(10);
        profile.setPadding(new Insets(12, 15, 12, 15));
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;");

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 28px; -fx-background-color: linear-gradient(to bottom right, #a855f7, #06b6d4); -fx-background-radius: 50; -fx-padding: 5 8 5 8;");

        VBox info = new VBox(2);
        Label name = new Label("Wishang Sakti");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label plan = new Label("Premium ✓");
        plan.setStyle("-fx-font-size: 11px; -fx-text-fill: #a855f7;");
        info.getChildren().addAll(name, plan);

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

        VBox titleBox = new VBox(4);
        sectionTitleLabel = new Label("Home");
        sectionTitleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        sectionSubtitleLabel = new Label("Welcome back! Ready to listen to some music?");
        sectionSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");
        titleBox.getChildren().addAll(sectionTitleLabel, sectionSubtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 25; -fx-padding: 8 15 8 15;");
        
        Label searchIcon = new Label("🔍");
        TextField searchField = new TextField();
        searchField.setPromptText("Search songs...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-font-size: 13px; -fx-pref-width: 180px; -fx-border-color: transparent;");
        searchBox.getChildren().addAll(searchIcon, searchField);

        addSongBtn = new Button("+ Add Song");
        addSongBtn.setStyle("-fx-background-color: linear-gradient(to right, #a855f7, #06b6d4); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 25; -fx-padding: 10 20 10 20; -fx-cursor: hand; -fx-border-color: transparent;");
        
        addSongBtn.setOnAction(e -> openAddSongDialog());

        header.getChildren().addAll(titleBox, spacer, searchBox, addSongBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        songListContainer = new VBox(2);
        songListContainer.setPadding(new Insets(5, 20, 20, 20));

        refreshSongList();
        scrollPane.setContent(songListContainer);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSongs.clear();
            if (newVal == null || newVal.isEmpty()) {
                restoreSongsByActiveMenu(sectionTitleLabel.getText());
            } else {
                String lower = newVal.toLowerCase();
                allSongs.stream()
                    .filter(s -> s.getTitle().toLowerCase().contains(lower) || s.getArtist().toLowerCase().contains(lower))
                    .forEach(filteredSongs::add);
            }
            refreshSongList();
        });

        HBox columnHeaders = new HBox();
        columnHeaders.setPadding(new Insets(5, 25, 8, 25));
        columnHeaders.setStyle("-fx-border-color: transparent transparent #1e1e3a transparent; -fx-border-width: 0 0 1 0;");
        String[] columnNames = {"#", "TITLE", "ALBUM", "GENRE", "TIME", ""};
        double[] widths = {40, 280, 180, 120, 60, 80};
        for (int i = 0; i < columnNames.length; i++) {
            Label col = new Label(columnNames[i]);
            col.setMinWidth(widths[i]); col.setMaxWidth(widths[i]);
            col.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");
            columnHeaders.getChildren().add(col);
        }

        container.getChildren().addAll(header, columnHeaders, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return container;
    }

    private void refreshSongList() {
        songListContainer.getChildren().clear();
        for (int i = 0; i < filteredSongs.size(); i++) {
            songListContainer.getChildren().add(createSongRow(filteredSongs.get(i), i + 1));
        }
    }

    // ============================================================
    // SONG ROW (FIXED)
    // ============================================================
    private HBox createSongRow(Song song, int number) {
        boolean isCurrentSong = (currentSong != null && currentSong.equals(song));

        HBox row = new HBox(0);
        row.setPadding(new Insets(10, 5, 10, 5));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setStyle(isCurrentSong
            ? "-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;"
            : "-fx-background-color: transparent;");

        // Kolom nomor
        Label numLabel = new Label(isCurrentSong ? "♪" : String.valueOf(number));
        numLabel.setMinWidth(40); numLabel.setMaxWidth(40);
        numLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isCurrentSong ? "#a855f7" : "rgba(255,255,255,0.4)") + ";");

        // Kolom judul & artis
        VBox titleBox = new VBox(3);
        titleBox.setMinWidth(280); titleBox.setMaxWidth(280);
        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (isCurrentSong ? "#a855f7" : "white") + ";");
        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.5);");
        titleBox.getChildren().addAll(titleLabel, artistLabel);

        // Kolom album
        Label albumLabel = new Label(song.getAlbum());
        albumLabel.setMinWidth(180); albumLabel.setMaxWidth(180);
        albumLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");

        // Kolom genre
        Label genreLabel = new Label(song.getGenre());
        genreLabel.setMinWidth(120); genreLabel.setMaxWidth(120);
        genreLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4); -fx-background-color: #ffffff10; -fx-background-radius: 10; -fx-padding: 2 8 2 8;");

        // Kolom durasi
        Label durationLabel = new Label(song.getDuration());
        durationLabel.setMinWidth(60); durationLabel.setMaxWidth(60);
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.4);");

        // Kolom aksi (like + delete)
        HBox actions = new HBox(12);
        actions.setMinWidth(80);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label likeBtn = new Label(song.isLiked() ? "❤️" : "🤍");
        likeBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        likeBtn.setOnMouseClicked(e -> {
            e.consume();
            song.setLiked(!song.isLiked());
            likeBtn.setText(song.isLiked() ? "❤️" : "🤍");
        });

        Label deleteBtn = new Label("🗑️");
        deleteBtn.setStyle("-fx-font-size: 15px; -fx-cursor: hand;");
        deleteBtn.setVisible(false);

        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Hapus Lagu Permanen");
            confirm.setHeaderText(null);
            confirm.setContentText("Yakin lu mau musnahin lagu '" + song.getTitle() + "' dari Supabase?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteSongFromSupabase(song);
                }
            });
        });

        actions.getChildren().addAll(likeBtn, deleteBtn);

        row.getChildren().addAll(numLabel, titleBox, albumLabel, genreLabel, durationLabel, actions);

        // Hover effect — pakai effectively final variable
        final boolean isCurrent = isCurrentSong;
        row.setOnMouseEntered(e -> {
            if (!isCurrent) row.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;");
            deleteBtn.setVisible(true);
        });
        row.setOnMouseExited(e -> {
            if (!isCurrent) row.setStyle("-fx-background-color: transparent;");
            deleteBtn.setVisible(false);
        });

        row.setOnMouseClicked(e -> playSong(song));

        return row;
    }

    // ============================================================
    // RIGHT PANEL (NOW PLAYING & ADD TO PLAYLIST)
    // ============================================================
    private VBox buildNowPlayingPanel() {
        VBox panel = new VBox(10); 
        panel.setPrefWidth(260); 
        panel.setPadding(new Insets(10, 15, 10, 15));
        panel.setStyle("-fx-background-color: #111127; -fx-border-color: transparent transparent transparent #1e1e3a; -fx-border-width: 0 0 0 1;");

        Label panelTitle = new Label("NOW PLAYING");
        panelTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        nowPlayingTitle = new Label(currentSong != null ? currentSong.getTitle() : "No Song");
        nowPlayingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");
        nowPlayingTitle.setMaxWidth(230);
        
        nowPlayingArtist = new Label(currentSong != null ? currentSong.getArtist() : "Unknown Artist");
        nowPlayingArtist.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");

        panel.getChildren().addAll(
            panelTitle, 
            createAlbumArt(), 
            nowPlayingTitle, 
            nowPlayingArtist, 
            buildProgressSection(), 
            buildVolumeSection(), 
            buildAddToPlaylistSection() 
        );
        return panel;
    }

    private StackPane createAlbumArt() {
        StackPane albumArt = new StackPane();
        albumArt.setPrefSize(180, 180); 
        
        Rectangle artBg = new Rectangle(170, 170);
        artBg.setArcWidth(25); artBg.setArcHeight(25);
        artBg.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#a855f7")), new Stop(1, Color.web("#06b6d4"))));
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#a855f760")); shadow.setRadius(20); shadow.setOffsetY(5);
        artBg.setEffect(shadow);
        
        Label bigEmoji = new Label("🎵");
        bigEmoji.setStyle("-fx-font-size: 60px;");
        
        albumArt.getChildren().addAll(artBg, bigEmoji);
        return albumArt;
    }

    private VBox buildProgressSection() {
        VBox section = new VBox(8);
        HBox timeRow = new HBox();
        currentTimeLabel = new Label("0:00");
        currentTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
        totalTimeLabel = new Label(currentSong != null ? currentSong.getDuration() : "0:00");
        totalTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        timeRow.getChildren().addAll(currentTimeLabel, sp, totalTimeLabel);

        seekBar = new ProgressBar(0);
        seekBar.setMaxWidth(Double.MAX_VALUE);
        seekBar.setStyle("-fx-accent: #a855f7; -fx-pref-height: 4px;");

        section.getChildren().addAll(timeRow, seekBar);
        return section;
    }

    private HBox buildVolumeSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);
        Label volIcon = new Label("🔊");
        Slider volumeSlider = new Slider(0, 1.0, 0.7);
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);

        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(val.doubleValue());
        });

        section.getChildren().addAll(volIcon, volumeSlider);
        return section;
    }

    private VBox buildAddToPlaylistSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(15, 0, 0, 0));

        Label label = new Label("ADD CURRENT SONG TO:");
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        playlistSelector.setPromptText("Choose playlist...");
        playlistSelector.setMaxWidth(Double.MAX_VALUE);
        playlistSelector.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 5;");
        
        playlistSelector.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white;");
            }
        });

        Button addToPlBtn = new Button("➕ Add to Selected Playlist");
        addToPlBtn.setMaxWidth(Double.MAX_VALUE);
        addToPlBtn.setStyle("-fx-background-color: #ffffff10; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 12; -fx-padding: 8; -fx-cursor: hand;");

        addToPlBtn.setOnAction(e -> {
            String selectedPlaylist = playlistSelector.getValue();
            if (selectedPlaylist == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Pilih Playlist");
                alert.setHeaderText(null);
                alert.setContentText("Wajib milih nama playlist dulu di menu drop-down atas bre!");
                alert.showAndWait();
                return;
            }
            if (currentSong != null) {
                ObservableList<Song> songsInPlaylist = playlistMap.get(selectedPlaylist);
                if (songsInPlaylist != null && !songsInPlaylist.contains(currentSong)) {
                    songsInPlaylist.add(currentSong);
                    updatePlaylistSidebarCounter(selectedPlaylist, songsInPlaylist.size());
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setHeaderText(null);
                    success.setContentText("Berhasil masukin lagu ke " + selectedPlaylist + "!");
                    success.show();
                }
            }
        });

        section.getChildren().addAll(label, playlistSelector, addToPlBtn);
        return section;
    }

    private void updatePlaylistSidebarCounter(String playlistName, int count) {
        for (Node node : customPlaylistContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                VBox infoVBox = (VBox) row.getChildren().get(1);
                Label nameLbl = (Label) infoVBox.getChildren().get(0);
                if (nameLbl.getText().equals(playlistName)) {
                    Label countLbl = (Label) infoVBox.getChildren().get(1);
                    countLbl.setText(count + " songs");
                    break;
                }
            }
        }
    }

    // ============================================================
    // PLAYER CONTROLS & MEDIA LOGIC
    // ============================================================
    private HBox buildPlayerControls() {
        HBox controls = new HBox(30);
        controls.setPadding(new Insets(15, 30, 20, 30));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3a transparent transparent transparent;");

        Label prevBtn = new Label("⏮"); prevBtn.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");
        playBtn = new Label("▶");
        playBtn.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #a855f7; -fx-background-radius: 50; -fx-padding: 10 15 10 15; -fx-cursor: hand;");
        Label nextBtn = new Label("⏭"); nextBtn.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-cursor: hand;");

        playBtn.setOnMouseClicked(e -> togglePlay());
        prevBtn.setOnMouseClicked(e -> handlePrev());
        nextBtn.setOnMouseClicked(e -> handleNext());

        controls.getChildren().addAll(prevBtn, playBtn, nextBtn);
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

            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    double total = mediaPlayer.getTotalDuration().toSeconds();
                    int totalMin = (int) total / 60;
                    int totalSec = (int) total % 60;
                    totalTimeLabel.setText(String.format("%d:%02d", totalMin, totalSec));
                });
            });

            mediaPlayer.play();
            mediaPlayer.setOnEndOfMedia(this::handleNext);

        } catch (Exception e) {
            System.err.println("Gagal memutar audio: " + e.getMessage());
        }
    }

    private void togglePlay() {
        if (currentSong == null && !allSongs.isEmpty()) {
            playSong(allSongs.get(0));
            return;
        }
        if (mediaPlayer == null) return;

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
        if (allSongs.isEmpty()) return;
        currentIndex = (currentIndex + 1) % allSongs.size();
        playSong(allSongs.get(currentIndex));
    }

    private void handlePrev() {
        if (allSongs.isEmpty()) return;
        currentIndex = (currentIndex - 1 + allSongs.size()) % allSongs.size();
        playSong(allSongs.get(currentIndex));
    }

    private void restoreSongsByActiveMenu(String currentMenu) {
        if (currentMenu.equals("Liked Songs")) {
            allSongs.stream().filter(Song::isLiked).forEach(filteredSongs::add);
        } else if (currentMenu.equals("Radio") || currentMenu.equals("Downloads")) {
            // Kosong
        } else {
            ObservableList<Song> saved = playlistMap.get(currentMenu);
            if (saved != null) filteredSongs.addAll(saved);
            else filteredSongs.addAll(allSongs);
        }
    }

    // ============================================================
    // WINDOW DRAG & ANIMATIONS
    // ============================================================
    private double xOffset = 0, yOffset = 0;
    private void enableDrag(Node root, Stage stage) {
        root.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        root.setOnMouseDragged(e -> { if (yOffset < 60) { stage.setX(e.getScreenX() - xOffset); stage.setY(e.getScreenY() - yOffset); } });
    }

    private void playEntryAnimation(Node root) {
        root.setOpacity(0); root.setScaleX(0.95); root.setScaleY(0.95);
        new ParallelTransition(createFadeIn(root, 500), createScaleIn(root, 500)).play();
    }

    private FadeTransition createFadeIn(Node node, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT); return ft;
    }

    private TranslateTransition createSlideRight(Node node, int ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT); return tt;
    }

    private ScaleTransition createScaleIn(Node node, int ms) {
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), node);
        st.setToX(1.0); st.setToY(1.0); st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)); return st;
    }

    // ============================================================
    // FITUR ADD SONG & BACKEND SUPABASE
    // ============================================================
    private void openAddSongDialog() {
        Dialog<Song> dialog = new Dialog<>();
        dialog.setTitle("Tambah Lagu Baru");
        dialog.setHeaderText("Masukkan detail lagu buat dikirim ke Supabase 🚀");
        dialog.initStyle(StageStyle.UTILITY);

        ButtonType saveButtonType = new ButtonType("Save to Database", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(); titleField.setPromptText("Judul Lagu (ex: Secukupnya)");
        TextField artistField = new TextField(); artistField.setPromptText("Artis (ex: Hindia)");
        TextField albumField = new TextField(); albumField.setPromptText("Album");
        TextField genreField = new TextField(); genreField.setPromptText("Genre (ex: Indie)");
        TextField durationField = new TextField(); durationField.setPromptText("Durasi (ex: 3:45)");
        TextField emojiField = new TextField(); emojiField.setPromptText("Emoji Foto (ex: 🎸)");
        TextField urlField = new TextField(); urlField.setPromptText("URL MP3 (Link Supabase Storage)");

        grid.add(new Label("Judul:"), 0, 0); grid.add(titleField, 1, 0);
        grid.add(new Label("Artis:"), 0, 1); grid.add(artistField, 1, 1);
        grid.add(new Label("Album:"), 0, 2); grid.add(albumField, 1, 2);
        grid.add(new Label("Genre:"), 0, 3); grid.add(genreField, 1, 3);
        grid.add(new Label("Durasi:"), 0, 4); grid.add(durationField, 1, 4);
        grid.add(new Label("Emoji Foto:"), 0, 5); grid.add(emojiField, 1, 5);
        grid.add(new Label("Link MP3:"), 0, 6); grid.add(urlField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Song(
                    titleField.getText(), artistField.getText(), albumField.getText(),
                    durationField.getText(), genreField.getText(), emojiField.getText()
                );
            }
            return null;
        });

        Optional<Song> result = dialog.showAndWait();

        result.ifPresent(newSong -> {
            newSong.setFileUrl(urlField.getText());
            saveSongToSupabase(newSong);
        });
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

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                allSongs.add(song);
                filteredSongs.add(song);
                refreshSongList();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Supabase Connected!");
                success.setHeaderText(null);
                success.setContentText("Lagu '" + song.getTitle() + "' berhasil mendarat di Supabase Cloud!");
                success.show();
            }

        } catch (Exception e) {
            allSongs.add(song);
            filteredSongs.add(song);
            refreshSongList();
            
            Alert error = new Alert(Alert.AlertType.WARNING);
            error.setTitle("Database Warning");
            error.setHeaderText("Supabase Belum Konek bre!");
            error.setContentText("Lagu berhasil ditambahkan di aplikasi lokal, tapi gagal masuk ke Supabase Cloud.\nError: " + e.getMessage());
            error.show();
            e.printStackTrace();
        }
    }

    // ============================================================
    // FITUR DELETE SONG DARI SUPABASE
    // ============================================================
    private void deleteSongFromSupabase(Song song) {
        String sql = "DELETE FROM songs WHERE title = ? AND artist = ?";

        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            
            pstmt.executeUpdate();

            allSongs.remove(song);
            filteredSongs.remove(song);
            
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

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setHeaderText(null);
            success.setContentText("Lagu '" + song.getTitle() + "' berhasil dilenyapkan dari muka bumi!");
            success.show();

        } catch (Exception e) {
            allSongs.remove(song);
            filteredSongs.remove(song);
            refreshSongList();
            
            Alert error = new Alert(Alert.AlertType.WARNING);
            error.setTitle("Database Warning");
            error.setHeaderText("Gagal hapus di awan!");
            error.setContentText("Lagu terhapus dari layar, tapi gagal dihapus dari Supabase Cloud.\nError: " + e.getMessage());
            error.show();
            e.printStackTrace();
        }
    }
}