package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
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
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    
    private Label nowPlayingTitle;
    private Label nowPlayingArtist;
    private Label playBtn;
    private ProgressBar seekBar;
    private Timeline progressTimeline;
    private double progressValue = 0;
    
    private VBox songListContainer;
    private Label currentTimeLabel;
    private Label totalTimeLabel;

    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;
        
        // Init data
        initSongData();
        
        // Build UI
        BorderPane root = buildMainUI();
        
        // Scene setup
        Scene scene = new Scene(root, 1200, 750);
        scene.setFill(Color.TRANSPARENT);
        
        // Stage setup
        stage.setTitle("🎵 Melodify - Music Player");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
        stage.centerOnScreen();
        
        // Entry animation
        playEntryAnimation(root);
        
        // Drag to move
        enableDrag(root, stage);
    }

    private void initSongData() {
        allSongs = FXCollections.observableArrayList(
            new Song("Blinding Lights", "The Weeknd", "After Hours", "3:20", "Synth-pop", "🌃"),
            new Song("As It Was", "Harry Styles", "Harry's House", "2:37", "Indie Pop", "🏠"),
            new Song("Stay", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3", "2:21", "Pop", "💫"),
            new Song("Heat Waves", "Glass Animals", "Dreamland", "3:59", "Indie Rock", "🌊"),
            new Song("Levitating", "Dua Lipa", "Future Nostalgia", "3:23", "Dance Pop", "🚀"),
            new Song("Peaches", "Justin Bieber", "Justice", "3:18", "R&B", "🍑"),
            new Song("Good 4 U", "Olivia Rodrigo", "SOUR", "2:58", "Pop Rock", "🎸"),
            new Song("Industry Baby", "Lil Nas X", "MONTERO", "3:32", "Hip-Hop", "🎺"),
            new Song("Bad Habits", "Ed Sheeran", "=", "3:51", "Dance Pop", "🌙"),
            new Song("Shivers", "Ed Sheeran", "=", "3:27", "Pop", "⚡"),
            new Song("MONTERO", "Lil Nas X", "MONTERO", "2:17", "Pop Rap", "🌈"),
            new Song("Butter", "BTS", "Butter", "2:42", "Dance Pop", "🧈"),
            new Song("Permission to Dance", "BTS", "Butter", "3:05", "Dance Pop", "💃"),
            new Song("Save Your Tears", "The Weeknd", "After Hours", "3:35", "Synth-pop", "💔"),
            new Song("Love Story", "Taylor Swift", "Fearless", "3:55", "Country Pop", "💕"),
            new Song("Watermelon Sugar", "Harry Styles", "Fine Line", "2:54", "Pop", "🍉"),
            new Song("drivers license", "Olivia Rodrigo", "SOUR", "4:02", "Indie Pop", "🚗"),
            new Song("traitor", "Olivia Rodrigo", "SOUR", "3:49", "Indie Pop", "🥀"),
            new Song("Happier Than Ever", "Billie Eilish", "HTE", "4:59", "Alternative", "🖤"),
            new Song("Therefore I Am", "Billie Eilish", "HTE", "2:54", "Electropop", "💭")
        );
        filteredSongs = FXCollections.observableArrayList(allSongs);
        currentSong = allSongs.get(0);
    }

    private BorderPane buildMainUI() {
        BorderPane root = new BorderPane();
        root.setStyle(
            "-fx-background-color: #0d0d1a;" +
            "-fx-background-radius: 20;"
        );

        // === LEFT SIDEBAR ===
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // === CENTER - PLAYLIST ===
        VBox center = buildPlaylistSection();
        root.setCenter(center);

        // === RIGHT - NOW PLAYING ===
        VBox rightPanel = buildNowPlayingPanel();
        root.setRight(rightPanel);

        // === BOTTOM - PLAYER CONTROLS ===
        HBox playerControls = buildPlayerControls();
        root.setBottom(playerControls);

        // === TOP - TITLE BAR ===
        HBox titleBar = buildTitleBar();
        root.setTop(titleBar);

        return root;
    }

    // ============================================================
    // TITLE BAR
    // ============================================================
    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(15, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: transparent;");

        Label logo = new Label("🎵 MELODIFY");
        logo.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI';"
        );
        
        DropShadow logoGlow = new DropShadow();
        logoGlow.setColor(Color.web("#a855f7"));
        logoGlow.setRadius(15);
        logo.setEffect(logoGlow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window controls
        Label minimize = createWindowBtn("—", "#f59e0b");
        Label maximize = createWindowBtn("⬜", "#22c55e");
        Label close = createWindowBtn("✕", "#ef4444");

        minimize.setOnMouseClicked(e -> stage.setIconified(true));
        maximize.setOnMouseClicked(e -> {
            stage.setMaximized(!stage.isMaximized());
        });
        close.setOnMouseClicked(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), 
                stage.getScene().getRoot());
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
        btn.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-background-color: " + color + "33;" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-background-color: " + color + "33;" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-cursor: hand;"
        ));
        
        // Scale animation on hover
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.2);
            st.setToY(1.2);
            st.play();
            btn.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 50;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-cursor: hand;"
            );
        });
        
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            btn.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-background-color: " + color + "33;" +
                "-fx-background-radius: 50;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-cursor: hand;"
            );
        });
        
        return btn;
    }

    // ============================================================
    // SIDEBAR
    // ============================================================
    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(10, 15, 20, 15));
        sidebar.setStyle(
            "-fx-background-color: #111127;" +
            "-fx-border-color: transparent #1e1e3a transparent transparent;" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Menu items
        String[][] menuItems = {
            {"🏠", "Home"},
            {"🔍", "Discover"},
            {"🎵", "My Playlist"},
            {"❤️", "Liked Songs"},
            {"📻", "Radio"},
            {"📁", "Library"},
            {"⬇️", "Downloads"},
        };

        for (int i = 0; i < menuItems.length; i++) {
            HBox item = createSidebarItem(menuItems[i][0], menuItems[i][1], i == 2);
            sidebar.getChildren().add(item);
            
            // Staggered entrance animation
            item.setOpacity(0);
            item.setTranslateX(-20);
            PauseTransition delay = new PauseTransition(Duration.millis(100 + i * 80));
            final HBox finalItem = item;
            delay.setOnFinished(e -> {
                ParallelTransition pt = new ParallelTransition(
                    createFadeIn(finalItem, 300),
                    createSlideRight(finalItem, 300)
                );
                pt.play();
            });
            delay.play();
        }

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a;");
        
        Label playlistHeader = new Label("MY PLAYLISTS");
        playlistHeader.setStyle(
            "-fx-font-size: 10px;" +
            "-fx-text-fill: rgba(255,255,255,0.3);" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 5 5 5;"
        );

        // Playlist items
        String[][] playlists = {
            {"💜", "Chill Vibes", "23 songs"},
            {"🔥", "Party Mix", "45 songs"},
            {"😴", "Sleep Music", "18 songs"},
            {"💪", "Workout", "31 songs"},
        };

        sidebar.getChildren().addAll(sep, playlistHeader);
        
        for (String[] pl : playlists) {
            HBox plItem = createPlaylistSidebarItem(pl[0], pl[1], pl[2]);
            sidebar.getChildren().add(plItem);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        // User profile at bottom
        HBox userProfile = createUserProfile();
        sidebar.getChildren().addAll(spacer, userProfile);

        return sidebar;
    }

    private HBox createSidebarItem(String icon, String text, boolean active) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);

        String activeBg = "-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410);" +
                          "-fx-background-radius: 12;";
        String hoverBg = "-fx-background-color: #ffffff10;-fx-background-radius: 12;";
        String normalBg = "-fx-background-color: transparent;-fx-background-radius: 12;";

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label textLabel = new Label(text);
        textLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: " + (active ? "bold" : "normal") + ";" +
            "-fx-text-fill: " + (active ? "white" : "rgba(255,255,255,0.6)") + ";" +
            "-fx-font-family: 'Segoe UI';"
        );

        if (active) {
            item.setStyle(activeBg);
            // Active indicator
            Rectangle indicator = new Rectangle(3, 20);
            indicator.setArcWidth(3);
            indicator.setArcHeight(3);
            indicator.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a855f7")),
                new Stop(1, Color.web("#06b6d4"))
            ));
            item.getChildren().addAll(indicator, iconLabel, textLabel);
        } else {
            item.setStyle(normalBg);
            item.getChildren().addAll(iconLabel, textLabel);
        }

        if (!active) {
            item.setOnMouseEntered(e -> {
                item.setStyle(hoverBg);
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconLabel);
                st.setToX(1.2);
                st.setToY(1.2);
                st.play();
            });
            item.setOnMouseExited(e -> {
                item.setStyle(normalBg);
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconLabel);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
        }

        return item;
    }

    private HBox createPlaylistSidebarItem(String emoji, String name, String count) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(8, 15, 8, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setStyle("-fx-background-radius: 10;");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-background-color: #ffffff15;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 6 8 6 8;"
        );

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.8);" +
            "-fx-font-family: 'Segoe UI';"
        );
        Label countLabel = new Label(count);
        countLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: rgba(255,255,255,0.4);"
        );
        info.getChildren().addAll(nameLabel, countLabel);

        item.getChildren().addAll(emojiLabel, info);

        item.setOnMouseEntered(e -> 
            item.setStyle("-fx-background-color: #ffffff08;-fx-background-radius: 10;"));
        item.setOnMouseExited(e -> 
            item.setStyle("-fx-background-radius: 10;"));

        return item;
    }

    private HBox createUserProfile() {
        HBox profile = new HBox(10);
        profile.setPadding(new Insets(12, 15, 12, 15));
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setStyle(
            "-fx-background-color: #ffffff08;" +
            "-fx-background-radius: 12;"
        );

        Label avatar = new Label("👤");
        avatar.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-background-color: linear-gradient(to bottom right, #a855f7, #06b6d4);" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 5 8 5 8;"
        );

        VBox info = new VBox(2);
        Label name = new Label("John Doe");
        name.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        );
        Label plan = new Label("Premium ✓");
        plan.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #a855f7;"
        );
        info.getChildren().addAll(name, plan);

        profile.getChildren().addAll(avatar, info);
        return profile;
    }

    // ============================================================
    // PLAYLIST CENTER
    // ============================================================
    private VBox buildPlaylistSection() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: #0d0d1a;");

        // Header
        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 25, 15, 25));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("My Playlist");
        title.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI';"
        );
        Label subtitle = new Label("20 songs • 1 hr 15 min");
        subtitle.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search bar
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 8 15 8 15;"
        );
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search songs...");
        searchField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.3);" +
            "-fx-font-size: 13px;" +
            "-fx-pref-width: 180px;" +
            "-fx-border-color: transparent;"
        );
        
        searchBox.getChildren().addAll(searchIcon, searchField);

        // Add to playlist button
        Button addBtn = new Button("+ Add Song");
        addBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #a855f7, #06b6d4);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 10 20 10 20;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: transparent;"
        );
        
        // Hover effect for add button
        addBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), addBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#a855f7"));
            glow.setRadius(15);
            addBtn.setEffect(glow);
        });
        
        addBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), addBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            addBtn.setEffect(null);
        });

        header.getChildren().addAll(titleBox, spacer, searchBox, addBtn);

        // Column headers
        HBox columnHeaders = buildColumnHeaders();

        // Song list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;" +
            "-fx-border-color: transparent;"
        );
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        songListContainer = new VBox(2);
        songListContainer.setPadding(new Insets(5, 20, 20, 20));
        songListContainer.setStyle("-fx-background-color: transparent;");

        // Build song rows
        refreshSongList();

        scrollPane.setContent(songListContainer);

        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSongs.clear();
            if (newVal == null || newVal.isEmpty()) {
                filteredSongs.addAll(allSongs);
            } else {
                String lower = newVal.toLowerCase();
                allSongs.stream()
                    .filter(s -> s.getTitle().toLowerCase().contains(lower) ||
                                 s.getArtist().toLowerCase().contains(lower) ||
                                 s.getAlbum().toLowerCase().contains(lower))
                    .forEach(filteredSongs::add);
            }
            refreshSongList();
        });

        container.getChildren().addAll(header, columnHeaders, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return container;
    }

    private HBox buildColumnHeaders() {
        HBox headers = new HBox();
        headers.setPadding(new Insets(5, 25, 8, 25));
        headers.setStyle(
            "-fx-border-color: transparent transparent #1e1e3a transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        String[] columnNames = {"#", "TITLE", "ALBUM", "GENRE", "TIME", ""};
        double[] widths = {40, 280, 180, 120, 60, 80};

        for (int i = 0; i < columnNames.length; i++) {
            Label col = new Label(columnNames[i]);
            col.setMinWidth(widths[i]);
            col.setMaxWidth(widths[i]);
            col.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: rgba(255,255,255,0.3);" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: 'Segoe UI';"
            );
            headers.getChildren().add(col);
        }

        return headers;
    }

    private void refreshSongList() {
        songListContainer.getChildren().clear();
        
        for (int i = 0; i < filteredSongs.size(); i++) {
            Song song = filteredSongs.get(i);
            HBox row = createSongRow(song, i + 1);
            songListContainer.getChildren().add(row);
            
            // Staggered animation
            row.setOpacity(0);
            row.setTranslateY(10);
            
            int delay = i * 50;
            PauseTransition pt = new PauseTransition(Duration.millis(delay));
            pt.setOnFinished(e -> {
                FadeTransition ft = new FadeTransition(Duration.millis(200), row);
                ft.setToValue(1);
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), row);
                tt.setToY(0);
                new ParallelTransition(ft, tt).play();
            });
            pt.play();
        }
    }

    private HBox createSongRow(Song song, int number) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 5, 10, 5));
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setStyle("-fx-background-radius: 12;");

        boolean isCurrentSong = song == currentSong;
        
        if (isCurrentSong) {
            row.setStyle(
                "-fx-background-color: linear-gradient(to right, #a855f715, #06b6d410);" +
                "-fx-background-radius: 12;"
            );
        }

        // Number column
        StackPane numberPane = new StackPane();
        numberPane.setMinWidth(40);
        numberPane.setMaxWidth(40);
        
        Label numLabel = new Label(isCurrentSong ? "▶" : String.valueOf(number));
        numLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: " + (isCurrentSong ? "#a855f7" : "rgba(255,255,255,0.4)") + ";"
        );
        
        // Playing animation for current song
        if (isCurrentSong && isPlaying) {
            numLabel.setText("▶");
            // Pulse animation
            FadeTransition pulse = new FadeTransition(Duration.millis(500), numLabel);
            pulse.setFromValue(0.4);
            pulse.setToValue(1.0);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
        
        numberPane.getChildren().add(numLabel);

        // Cover + Title column
        HBox titleSection = new HBox(12);
        titleSection.setMinWidth(280);
        titleSection.setMaxWidth(280);
        titleSection.setAlignment(Pos.CENTER_LEFT);

        // Cover
        StackPane cover = new StackPane();
        cover.setPrefSize(42, 42);
        cover.setMinSize(42, 42);
        Rectangle coverBg = new Rectangle(42, 42);
        coverBg.setArcWidth(10);
        coverBg.setArcHeight(10);
        
        // Random gradient for each song
        Color[] gradients = {
            Color.web("#a855f7"), Color.web("#06b6d4"), Color.web("#f59e0b"),
            Color.web("#ef4444"), Color.web("#22c55e"), Color.web("#ec4899"),
            Color.web("#8b5cf6"), Color.web("#14b8a6")
        };
        Color c1 = gradients[number % gradients.length];
        Color c2 = gradients[(number + 3) % gradients.length];
        coverBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, c1), new Stop(1, c2)));
        
        Label coverEmoji = new Label(song.getCoverEmoji());
        coverEmoji.setStyle("-fx-font-size: 18px;");
        
        cover.getChildren().addAll(coverBg, coverEmoji);

        // Song info
        VBox songInfo = new VBox(3);
        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: " + (isCurrentSong ? "bold" : "normal") + ";" +
            "-fx-text-fill: " + (isCurrentSong ? "#a855f7" : "white") + ";" +
            "-fx-font-family: 'Segoe UI';"
        );
        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );
        songInfo.getChildren().addAll(titleLabel, artistLabel);
        titleSection.getChildren().addAll(cover, songInfo);

        // Album column
        Label albumLabel = new Label(song.getAlbum());
        albumLabel.setMinWidth(180);
        albumLabel.setMaxWidth(180);
        albumLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );

        // Genre column
        Label genreLabel = new Label(song.getGenre());
        genreLabel.setMinWidth(120);
        genreLabel.setMaxWidth(120);
        genreLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: " + getGenreColor(song.getGenre()) + ";" +
            "-fx-background-color: " + getGenreColor(song.getGenre()) + "20;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 3 8 3 8;"
        );

        // Duration column
        Label durLabel = new Label(song.getDuration());
        durLabel.setMinWidth(60);
        durLabel.setMaxWidth(60);
        durLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );

        // Actions column
        HBox actions = new HBox(8);
        actions.setMinWidth(80);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setOpacity(0); // Hidden by default

        Label likeBtn = new Label(song.isLiked() ? "❤️" : "🤍");
        likeBtn.setStyle("-fx-font-size: 16px;-fx-cursor: hand;");
        likeBtn.setOnMouseClicked(e -> {
            e.consume();
            song.setLiked(!song.isLiked());
            likeBtn.setText(song.isLiked() ? "❤️" : "🤍");
            // Heart animation
            ScaleTransition heartBeat = new ScaleTransition(Duration.millis(200), likeBtn);
            heartBeat.setToX(1.5);
            heartBeat.setToY(1.5);
            heartBeat.setAutoReverse(true);
            heartBeat.setCycleCount(2);
            heartBeat.play();
        });

        Label moreBtn = new Label("⋯");
        moreBtn.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-text-fill: rgba(255,255,255,0.6);" +
            "-fx-cursor: hand;"
        );

        actions.getChildren().addAll(likeBtn, moreBtn);

        row.getChildren().addAll(numberPane, titleSection, albumLabel, 
                                   genreLabel, durLabel, actions);

        // Hover effects
        row.setOnMouseEntered(e -> {
            if (song != currentSong) {
                row.setStyle(
                    "-fx-background-color: #ffffff08;" +
                    "-fx-background-radius: 12;"
                );
            }
            FadeTransition ft = new FadeTransition(Duration.millis(150), actions);
            ft.setToValue(1);
            ft.play();
            numLabel.setText("▶");
        });

        row.setOnMouseExited(e -> {
            if (song != currentSong) {
                row.setStyle("-fx-background-radius: 12;");
            }
            FadeTransition ft = new FadeTransition(Duration.millis(150), actions);
            ft.setToValue(0);
            ft.play();
            if (song != currentSong) {
                numLabel.setText(String.valueOf(number));
            }
        });

        // Click to play
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || true) {
                playSong(song);
            }
        });

        return row;
    }

    private String getGenreColor(String genre) {
        return switch (genre) {
            case "Synth-pop", "Dance Pop" -> "#06b6d4";
            case "Indie Pop", "Indie Rock" -> "#22c55e";
            case "Pop", "Pop Rock" -> "#ec4899";
            case "Hip-Hop", "Pop Rap" -> "#f59e0b";
            case "R&B" -> "#a855f7";
            case "Alternative", "Electropop" -> "#8b5cf6";
            case "Country Pop" -> "#f97316";
            default -> "#6366f1";
        };
    }

    // ============================================================
    // NOW PLAYING PANEL (RIGHT)
    // ============================================================
    private VBox buildNowPlayingPanel() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(280);
        panel.setPadding(new Insets(10, 20, 20, 20));
        panel.setStyle(
            "-fx-background-color: #111127;" +
            "-fx-border-color: transparent transparent transparent #1e1e3a;" +
            "-fx-border-width: 0 0 0 1;"
        );

        Label panelTitle = new Label("NOW PLAYING");
        panelTitle.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.3);" +
            "-fx-letter-spacing: 2px;"
        );

        // Album art
        StackPane albumArt = createAlbumArt();

        // Song info
        nowPlayingTitle = new Label(currentSong.getTitle());
        nowPlayingTitle.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-wrap-text: true;"
        );
        nowPlayingTitle.setMaxWidth(240);

        nowPlayingArtist = new Label(currentSong.getArtist());
        nowPlayingArtist.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );

        // Like + add buttons
        HBox songActions = new HBox(12);
        Label heartBtn = new Label("🤍");
        heartBtn.setStyle("-fx-font-size: 22px;-fx-cursor: hand;");
        heartBtn.setOnMouseClicked(e -> {
            currentSong.setLiked(!currentSong.isLiked());
            heartBtn.setText(currentSong.isLiked() ? "❤️" : "🤍");
            ScaleTransition hb = new ScaleTransition(Duration.millis(150), heartBtn);
            hb.setToX(1.5);
            hb.setToY(1.5);
            hb.setAutoReverse(true);
            hb.setCycleCount(2);
            hb.play();
        });
        
        Label addToBtn = new Label("➕");
        addToBtn.setStyle("-fx-font-size: 20px;-fx-cursor: hand;");
        
        Label shareBtn = new Label("↗️");
        shareBtn.setStyle("-fx-font-size: 20px;-fx-cursor: hand;");
        
        songActions.getChildren().addAll(heartBtn, addToBtn, shareBtn);
        songActions.setAlignment(Pos.CENTER_LEFT);

        // Progress bar section
        VBox progressSection = buildProgressSection();

        // Volume section
        HBox volumeSection = buildVolumeSection();

        // Up next section
        VBox upNext = buildUpNextSection();

        panel.getChildren().addAll(
            panelTitle, albumArt, nowPlayingTitle, nowPlayingArtist, 
            songActions, progressSection, volumeSection, upNext
        );

        return panel;
    }

    private StackPane createAlbumArt() {
        StackPane albumArt = new StackPane();
        albumArt.setPrefSize(240, 240);
        albumArt.setMaxSize(240, 240);

        // Outer glow ring
        Circle glowRing = new Circle(120);
        glowRing.setFill(Color.TRANSPARENT);
        glowRing.setStroke(Color.web("#a855f730"));
        glowRing.setStrokeWidth(1);

        // Background
        Rectangle artBg = new Rectangle(220, 220);
        artBg.setArcWidth(25);
        artBg.setArcHeight(25);
        artBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#a855f7")),
            new Stop(0.5, Color.web("#6366f1")),
            new Stop(1, Color.web("#06b6d4"))
        ));

        DropShadow artShadow = new DropShadow();
        artShadow.setColor(Color.web("#a855f7"));
        artShadow.setRadius(30);
        artShadow.setSpread(0.1);
        artBg.setEffect(artShadow);

        // Emoji
        Label coverEmoji = new Label(currentSong.getCoverEmoji());
        coverEmoji.setStyle("-fx-font-size: 80px;");

        // Spinning animation ring
        Arc spinRing = new Arc(0, 0, 115, 115, 0, 270);
        spinRing.setType(ArcType.OPEN);
        spinRing.setFill(Color.TRANSPARENT);
        spinRing.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#a855f7")),
            new Stop(1, Color.web("#06b6d4"))
        ));
        spinRing.setStrokeWidth(2);
        spinRing.setStrokeLineCap(StrokeLineCap.ROUND);

        RotateTransition ringRotate = new RotateTransition(Duration.seconds(3), spinRing);
        ringRotate.setByAngle(360);
        ringRotate.setCycleCount(Animation.INDEFINITE);
        ringRotate.setInterpolator(Interpolator.LINEAR);
        ringRotate.play();

        albumArt.getChildren().addAll(glowRing, artBg, coverEmoji, spinRing);

        // Floating animation
        TranslateTransition float1 = new TranslateTransition(Duration.seconds(3), albumArt);
        float1.setByY(-8);
        float1.setCycleCount(Animation.INDEFINITE);
        float1.setAutoReverse(true);
        float1.setInterpolator(Interpolator.EASE_BOTH);
        float1.play();

        return albumArt;
    }

    private VBox buildProgressSection() {
        VBox section = new VBox(8);

        HBox timeRow = new HBox();
        currentTimeLabel = new Label("0:00");
        currentTimeLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalTimeLabel = new Label(currentSong.getDuration());
        totalTimeLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: rgba(255,255,255,0.5);"
        );
        timeRow.getChildren().addAll(currentTimeLabel, spacer, totalTimeLabel);

        // Custom progress bar
        StackPane progressContainer = new StackPane();
        progressContainer.setMaxHeight(6);
        progressContainer.setPrefHeight(6);

        Rectangle progressTrack = new Rectangle();
        progressTrack.setHeight(4);
        progressTrack.setArcWidth(4);
        progressTrack.setArcHeight(4);
        progressTrack.setFill(Color.web("#ffffff20"));
        progressTrack.widthProperty().bind(progressContainer.widthProperty());

        seekBar = new ProgressBar(0);
        seekBar.setPrefHeight(4);
        seekBar.setMaxWidth(Double.MAX_VALUE);
        seekBar.setStyle(
            "-fx-accent: #a855f7;" +
            "-fx-background-color: transparent;" +
            "-fx-pref-height: 4px;"
        );
        
        // Glow on progress
        DropShadow progressGlow = new DropShadow();
        progressGlow.setColor(Color.web("#a855f7"));
        progressGlow.setRadius(6);
        seekBar.setEffect(progressGlow);

        // Thumb indicator
        Circle thumb = new Circle(6);
        thumb.setFill(Color.WHITE);
        thumb.setStroke(Color.web("#a855f7"));
        thumb.setStrokeWidth(2);
        DropShadow thumbGlow = new DropShadow();
        thumbGlow.setColor(Color.web("#a855f7"));
        thumbGlow.setRadius(8);
        thumb.setEffect(thumbGlow);
        thumb.setOpacity(0);

        progressContainer.setOnMouseEntered(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), thumb);
            ft.setToValue(1);
            ft.play();
        });
        progressContainer.setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), thumb);
            ft.setToValue(0);
            ft.play();
        });

        progressContainer.getChildren().addAll(progressTrack, seekBar, thumb);
        section.getChildren().addAll(timeRow, progressContainer);

        return section;
    }

    private HBox buildVolumeSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);

        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-font-size: 14px;");

        Slider volumeSlider = new Slider(0, 100, 70);
        volumeSlider.setStyle(
            "-fx-accent: linear-gradient(to right, #a855f7, #06b6d4);"
        );
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);
        volumeSlider.setStyle(
            "-fx-accent: #06b6d4;" +
            "-fx-pref-height: 4px;"
        );

        Label volLevel = new Label("70%");
        volLevel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: rgba(255,255,255,0.4);"
        );

        volumeSlider.valueProperty().addListener((obs, old, val) -> 
            volLevel.setText((int)val.doubleValue() + "%"));

        section.getChildren().addAll(volIcon, volumeSlider, volLevel);
        return section;
    }

    private VBox buildUpNextSection() {
        VBox section = new VBox(10);

        Label upNextTitle = new Label("UP NEXT");
        upNextTitle.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.3);" +
            "-fx-padding: 10 0 0 0;"
        );

        section.getChildren().add(upNextTitle);

        int nextIdx = (currentIndex + 1) % allSongs.size();
        for (int i = 0; i < 3; i++) {
            Song next = allSongs.get((nextIdx + i) % allSongs.size());
            HBox item = new HBox(10);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(6, 8, 6, 8));
            item.setStyle(
                "-fx-background-color: #ffffff05;" +
                "-fx-background-radius: 8;"
            );

            Label emoji = new Label(next.getCoverEmoji());
            emoji.setStyle("-fx-font-size: 20px;");

            VBox info = new VBox(2);
            Label songName = new Label(next.getTitle());
            songName.setStyle("-fx-font-size: 12px;-fx-text-fill: rgba(255,255,255,0.8);");
            Label artistName = new Label(next.getArtist());
            artistName.setStyle("-fx-font-size: 11px;-fx-text-fill: rgba(255,255,255,0.4);");
            info.getChildren().addAll(songName, artistName);

            item.getChildren().addAll(emoji, info);
            section.getChildren().add(item);
        }

        return section;
    }

    // ============================================================
    // PLAYER CONTROLS (BOTTOM)
    // ============================================================
    private HBox buildPlayerControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15, 30, 20, 30));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle(
            "-fx-background-color: #0d0d1a;" +
            "-fx-border-color: #1e1e3a transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        // Left: shuffle and prev
        HBox leftControls = new HBox(15);
        leftControls.setAlignment(Pos.CENTER);
        
        Label shuffleBtn = createControlBtn("🔀", false);
        Label prevBtn = createControlBtn("⏮", false);

        shuffleBtn.setOnMouseClicked(e -> {
            isShuffle = !isShuffle;
            shuffleBtn.setStyle(shuffleBtn.getStyle().replace(
                isShuffle ? "rgba(255,255,255,0.7)" : "#a855f7",
                isShuffle ? "#a855f7" : "rgba(255,255,255,0.7)"
            ));
        });
        
        prevBtn.setOnMouseClicked(e -> {
            currentIndex = (currentIndex - 1 + allSongs.size()) % allSongs.size();
            playSong(allSongs.get(currentIndex));
        });

        leftControls.getChildren().addAll(shuffleBtn, prevBtn);

        // Center: PLAY button (big)
        playBtn = new Label(isPlaying ? "⏸" : "▶");
        playBtn.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: linear-gradient(to bottom right, #a855f7, #6366f1);" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 15 18 15 18;" +
            "-fx-cursor: hand;"
        );
        
        DropShadow playGlow = new DropShadow();
        playGlow.setColor(Color.web("#a855f7"));
        playGlow.setRadius(20);
        playGlow.setSpread(0.2);
        playBtn.setEffect(playGlow);

        playBtn.setOnMouseClicked(e -> togglePlay());
        playBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), playBtn);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });
        playBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), playBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        // Right: next and repeat
        HBox rightControls = new HBox(15);
        rightControls.setAlignment(Pos.CENTER);
        
        Label nextBtn = createControlBtn("⏭", false);
        Label repeatBtn = createControlBtn("🔁", false);

        nextBtn.setOnMouseClicked(e -> {
            currentIndex = (currentIndex + 1) % allSongs.size();
            playSong(allSongs.get(currentIndex));
        });

        rightControls.getChildren().addAll(nextBtn, repeatBtn);

        // Equal width regions
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        controls.getChildren().addAll(
            leftControls, leftSpacer, playBtn, rightSpacer, rightControls
        );

        return controls;
    }

    private Label createControlBtn(String icon, boolean active) {
        Label btn = new Label(icon);
        btn.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-text-fill: rgba(255,255,255,0.7);" +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.3);
            st.setToY(1.3);
            st.play();
            btn.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-text-fill: white;" +
                "-fx-cursor: hand;"
            );
        });
        
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            btn.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-text-fill: rgba(255,255,255,0.7);" +
                "-fx-cursor: hand;"
            );
        });
        
        return btn;
    }

    // ============================================================
    // PLAYER LOGIC
    // ============================================================
    private void playSong(Song song) {
        currentSong = song;
        currentIndex = allSongs.indexOf(song);
        isPlaying = true;
        
        // Update now playing panel
        nowPlayingTitle.setText(song.getTitle());
        nowPlayingArtist.setText(song.getArtist());
        playBtn.setText("⏸");
        totalTimeLabel.setText(song.getDuration());
        
        // Reset progress
        progressValue = 0;
        if (seekBar != null) seekBar.setProgress(0);
        if (currentTimeLabel != null) currentTimeLabel.setText("0:00");
        
        // Refresh song list
        refreshSongList();
        
        // Start progress simulation
        startProgressSimulation(song);
    }

    private void togglePlay() {
        isPlaying = !isPlaying;
        playBtn.setText(isPlaying ? "⏸" : "▶");
        
        if (isPlaying) {
            startProgressSimulation(currentSong);
            // Bounce animation
            ScaleTransition bounce = new ScaleTransition(Duration.millis(200), playBtn);
            bounce.setToX(0.9);
            bounce.setToY(0.9);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(2);
            bounce.play();
        } else {
            if (progressTimeline != null) progressTimeline.pause();
        }
    }

    private void startProgressSimulation(Song song) {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
        
        // Parse duration
        String dur = song.getDuration();
        String[] parts = dur.split(":");
        int totalSeconds = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        
        progressTimeline = new Timeline();
        progressTimeline.setCycleCount(totalSeconds * 10);
        
        for (int i = 1; i <= totalSeconds * 10; i++) {
            final int tick = i;
            KeyFrame kf = new KeyFrame(Duration.millis(100 * i), e -> {
                double progress = (double) tick / (totalSeconds * 10);
                seekBar.setProgress(progress);
                
                int elapsed = tick / 10;
                int min = elapsed / 60;
                int sec = elapsed % 60;
                currentTimeLabel.setText(String.format("%d:%02d", min, sec));
            });
            progressTimeline.getKeyFrames().add(kf);
        }
        
        progressTimeline.setOnFinished(e -> {
            // Auto next
            currentIndex = (currentIndex + 1) % allSongs.size();
            playSong(allSongs.get(currentIndex));
        });
        
        if (isPlaying) progressTimeline.play();
    }

    // ============================================================
    // ENTRY ANIMATION
    // ============================================================
    private void playEntryAnimation(Node root) {
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);
        
        ParallelTransition entry = new ParallelTransition(
            createFadeIn(root, 500),
            createScaleIn(root, 500)
        );
        entry.play();
    }

    // ============================================================
    // DRAG WINDOW
    // ============================================================
    private double xOffset = 0, yOffset = 0;
    
    private void enableDrag(Node root, Stage stage) {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            if (yOffset < 60) { // Only drag from title bar area
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    // ============================================================
    // ANIMATION HELPERS
    // ============================================================
    private FadeTransition createFadeIn(Node node, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        return ft;
    }

    private TranslateTransition createSlideRight(Node node, int ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        return tt;
    }

    private ScaleTransition createScaleIn(Node node, int ms) {
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), node);
        st.setToX(1.0);
        st.setToY(1.0);
        // Cari di baris 1416
        st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        return st;
    }
}
