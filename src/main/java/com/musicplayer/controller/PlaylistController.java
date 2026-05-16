package com.musicplayer.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.musicplayer.model.DatabaseConfig; // Pastikan file DatabaseConfig.java sudah lu buat di folder model
import com.musicplayer.model.Song;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
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
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    
    private Label nowPlayingTitle;
    private Label nowPlayingArtist;
    private Label playBtn;
    private ProgressBar seekBar;
    private Timeline progressTimeline;
    
    
    private VBox songListContainer;
    private Label currentTimeLabel;
    private Label totalTimeLabel;

    // FITUR UTAMA AUDIO PLAYER REAL
    private MediaPlayer mediaPlayer;
    private Slider volumeSlider;

    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;
        
        // Panggil data lokal dulu biar cepet masuk
        initFallbackLocalData();
        
        BorderPane root = buildMainUI();
        
        // FIX FINAL RESOLUSI: Lebar 1280, Tinggi 680 (Pasti pas di layar laptop!)
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
    
    // FIX: Sinkronisasi ke Supabase PostgreSQL via JDBC
    private void loadSongsFromSupabase() {
        allSongs.clear();
        // Pastikan tabel di Supabase lu namanya 'songs' dengan kolom berikut:
        String query = "SELECT id, title, artist, album, duration, genre, cover_emoji, file_url FROM songs ORDER BY id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Song song = new Song(
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("album"),
                    rs.getString("duration"),
                    rs.getString("genre"),
                    rs.getString("cover_emoji")
                );
                // Kita simpan URL Mp3 Supabase Storage / External Hosting ke dalam property song
                song.setFileUrl(rs.getString("file_url")); 
                allSongs.add(song);
            }
            System.out.println("LOG: Sukses memuat " + allSongs.size() + " lagu dari Supabase Cloud!");

        } catch (Exception e) {
            System.err.println("LOG ERROR: Gagal mengambil data dari Supabase. Menggunakan fallback data lokal.");
            e.printStackTrace();
            // Fallback biar proyek lu gak langsung blank kalau internet mati saat testing
            initFallbackLocalData();
        }

        filteredSongs.clear();
        filteredSongs.addAll(allSongs);
        if (!allSongs.isEmpty()) {
            currentSong = allSongs.get(0);
        }
    }

    private void initFallbackLocalData() {
        allSongs.clear();
        allSongs.add(new Song("Blinding Lights", "The Weeknd", "After Hours", "3:20", "Synth-pop", "🌃"));
        allSongs.get(0).setFileUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
        allSongs.add(new Song("As It Was", "Harry Styles", "Harry's House", "2:37", "Indie Pop", "🏠"));
        allSongs.get(1).setFileUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3");
        
        filteredSongs.clear();
        filteredSongs.addAll(allSongs);
        if (!allSongs.isEmpty()) {
            currentSong = allSongs.get(0);
        }
    }

    private BorderPane buildMainUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0d1a; -fx-background-radius: 20;");

        root.setLeft(buildSidebar());
        root.setCenter(buildPlaylistSection());
        root.setRight(buildNowPlayingPanel());
        root.setBottom(buildPlayerControls());
        root.setTop(buildTitleBar());

        return root;
    }

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
        
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.2); st.setToY(1.2); st.play();
            btn.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-background-radius: 50; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        });
        
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0); st.setToY(1.0); st.play();
            btn.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-background-color: " + color + "33; -fx-background-radius: 50; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        });
        
        return btn;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(10, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #111127; -fx-border-color: transparent #1e1e3a transparent transparent; -fx-border-width: 0 1 0 0;");

        String[][] menuItems = {
            {"🏠", "Home"}, {"🔍", "Discover"}, {"🎵", "My Playlist"},
            {"❤️", "Liked Songs"}, {"📻", "Radio"}, {"📁", "Library"}, {"⬇️", "Downloads"},
        };

        for (int i = 0; i < menuItems.length; i++) {
            HBox item = createSidebarItem(menuItems[i][0], menuItems[i][1], i == 2);
            sidebar.getChildren().add(item);
            
            item.setOpacity(0);
            item.setTranslateX(-20);
            PauseTransition delay = new PauseTransition(Duration.millis(100 + i * 50));
            final HBox finalItem = item;
            delay.setOnFinished(e -> {
                ParallelTransition pt = new ParallelTransition(createFadeIn(finalItem, 200), createSlideRight(finalItem, 200));
                pt.play();
            });
            delay.play();
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a;");
        
        Label playlistHeader = new Label("MY PLAYLISTS");
        playlistHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold; -fx-padding: 15 5 5 5;");

        sidebar.getChildren().addAll(sep, playlistHeader);
        
        String[][] playlists = {{"💜", "Chill Vibes", "23 songs"}, {"🔥", "Party Mix", "45 songs"}};
        for (String[] pl : playlists) {
            sidebar.getChildren().add(createPlaylistSidebarItem(pl[0], pl[1], pl[2]));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, createUserProfile());

        return sidebar;
    }

    private HBox createSidebarItem(String icon, String text, boolean active) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (active ? "bold" : "normal") + "; -fx-text-fill: " + (active ? "white" : "rgba(255,255,255,0.6)") + "; -fx-font-family: 'Segoe UI';");

        if (active) {
            item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
            Rectangle indicator = new Rectangle(3, 20);
            indicator.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#a855f7")), new Stop(1, Color.web("#06b6d4"))));
            item.getChildren().addAll(indicator, iconLabel, textLabel);
        } else {
            item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
            item.getChildren().addAll(iconLabel, textLabel);
            
            item.setOnMouseEntered(e -> {
                item.setStyle("-fx-background-color: #ffffff10; -fx-background-radius: 12;");
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconLabel);
                st.setToX(1.2); st.setToY(1.2); st.play();
            });
            item.setOnMouseExited(e -> {
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconLabel);
                st.setToX(1.0); st.setToY(1.0); st.play();
            });
        }

    item.setOnMouseClicked(e -> {
            if (text.equals("Liked Songs")) {
                // Filter cuma nampilin lagu yang di-like (❤️)
                filteredSongs.clear();
                allSongs.stream().filter(Song::isLiked).forEach(filteredSongs::add);
                refreshSongList();
            } else if (text.equals("My Playlist") || text.equals("Home")) {
                // Tampilain semua lagu lagi
                filteredSongs.clear();
                filteredSongs.addAll(allSongs);
                refreshSongList();
            }
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

        item.getChildren().addAll(emojiLabel, info);
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 10;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent;"));
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

    private VBox buildPlaylistSection() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: #0d0d1a;");

        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 25, 15, 25));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("My Playlist");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        Label subtitle = new Label("Cloud Database Synchronized");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");
        titleBox.getChildren().addAll(title, subtitle);

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

        header.getChildren().addAll(titleBox, spacer, searchBox);

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
                filteredSongs.addAll(allSongs);
            } else {
                String lower = newVal.toLowerCase();
                allSongs.stream()
                    .filter(s -> s.getTitle().toLowerCase().contains(lower) || s.getArtist().toLowerCase().contains(lower))
                    .forEach(filteredSongs::add);
            }
            refreshSongList();
        });

        container.getChildren().addAll(header, buildColumnHeaders(), scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSongs.clear();
            if (newVal == null || newVal.isEmpty()) {
                filteredSongs.addAll(allSongs);
            } else {
                String lower = newVal.toLowerCase();
                allSongs.stream()
                    .filter(s -> s.getTitle().toLowerCase().contains(lower) || 
                                s.getArtist().toLowerCase().contains(lower))
                    .forEach(filteredSongs::add);
            }
            refreshSongList();
        });
        return container;
    }

    private HBox buildColumnHeaders() {
        HBox headers = new HBox();
        headers.setPadding(new Insets(5, 25, 8, 25));
        headers.setStyle("-fx-border-color: transparent transparent #1e1e3a transparent; -fx-border-width: 0 0 1 0;");

        String[] columnNames = {"#", "TITLE", "ALBUM", "GENRE", "TIME", ""};
        double[] widths = {40, 280, 180, 120, 60, 80};

        for (int i = 0; i < columnNames.length; i++) {
            Label col = new Label(columnNames[i]);
            col.setMinWidth(widths[i]); col.setMaxWidth(widths[i]);
            col.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");
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
            row.setStyle("-fx-background-color: linear-gradient(to right, #a855f715, #06b6d410); -fx-background-radius: 12;");
        }

        StackPane numberPane = new StackPane();
        numberPane.setMinWidth(40);
        Label numLabel = new Label(isCurrentSong ? "▶" : String.valueOf(number));
        numLabel.setStyle("-fx-text-fill: " + (isCurrentSong ? "#a855f7" : "rgba(255,255,255,0.4)") + ";");
        numberPane.getChildren().add(numLabel);

        HBox titleSection = new HBox(12);
        titleSection.setMinWidth(280);
        titleSection.setAlignment(Pos.CENTER_LEFT);

        StackPane cover = new StackPane();
        cover.setPrefSize(42, 42);
        Rectangle coverBg = new Rectangle(42, 42);
        coverBg.setArcWidth(10); coverBg.setArcHeight(10);
        coverBg.setFill(Color.web("#a855f7"));
        Label coverEmoji = new Label(song.getCoverEmoji() != null ? song.getCoverEmoji() : "🎵");
        cover.getChildren().addAll(coverBg, coverEmoji);

        VBox songInfo = new VBox(3);
        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-text-fill: " + (isCurrentSong ? "#a855f7" : "white") + "; -fx-font-weight: bold;");
        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");
        songInfo.getChildren().addAll(titleLabel, artistLabel);
        titleSection.getChildren().addAll(cover, songInfo);

        Label albumLabel = new Label(song.getAlbum());
        albumLabel.setMinWidth(180); albumLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");

        Label genreLabel = new Label(song.getGenre());
        genreLabel.setMinWidth(120); genreLabel.setStyle("-fx-text-fill: #06b6d4; -fx-background-color: #06b6d415; -fx-background-radius: 20; -fx-padding: 3 8 3 8; -fx-font-size: 11px;");

        Label durLabel = new Label(song.getDuration());
        durLabel.setMinWidth(60); durLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");

        row.getChildren().addAll(numberPane, titleSection, albumLabel, genreLabel, durLabel);

        row.setOnMouseEntered(e -> {
            if (!isCurrentSong) row.setStyle("-fx-background-color: #ffffff08; -fx-background-radius: 12;");
        });
        row.setOnMouseExited(e -> {
            if (!isCurrentSong) row.setStyle("-fx-background-color: transparent;");
        });

        row.setOnMouseClicked(e -> playSong(song));
        return row;
    }

    private VBox buildNowPlayingPanel() {
        // SPASI DIPADATKAN JADI 10
        VBox panel = new VBox(10); 
        panel.setPrefWidth(260); // Agak dirampingin dikit
        panel.setPadding(new Insets(10, 15, 10, 15));
        panel.setStyle(
            "-fx-background-color: #111127;" + 
            "-fx-border-color: transparent transparent transparent #1e1e3a;" + 
            "-fx-border-width: 0 0 0 1;"
        );

        Label panelTitle = new Label("NOW PLAYING");
        panelTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        nowPlayingTitle = new Label("No Song");
        nowPlayingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        nowPlayingTitle.setWrapText(true);
        nowPlayingTitle.setMaxWidth(230);
        
        nowPlayingArtist = new Label("Unknown Artist");
        nowPlayingArtist.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");

        // Langsung masukin semua tanpa spacer aneh-aneh
        panel.getChildren().addAll(
            panelTitle, 
            createAlbumArt(), 
            nowPlayingTitle, 
            nowPlayingArtist, 
            buildProgressSection(), 
            buildVolumeSection(), 
            buildUpNextSection()
        );
        return panel;
    }

   private StackPane createAlbumArt() {
        StackPane albumArt = new StackPane();
        // UKURAN DIKECILIN BIAR GAK MENDORONG LAYAR KE BAWAH
        albumArt.setPrefSize(180, 180); 
        
        Rectangle artBg = new Rectangle(170, 170);
        artBg.setArcWidth(25); 
        artBg.setArcHeight(25);
        artBg.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, 
            new Stop(0, Color.web("#a855f7")), 
            new Stop(1, Color.web("#06b6d4"))));
        
        // EFEK BAYANGAN BIAR LEBIH MENARIK (3D Look)
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#a855f760"));
        shadow.setRadius(20);
        shadow.setOffsetY(5);
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
        totalTimeLabel = new Label("0:00");
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
        volumeSlider = new Slider(0, 1.0, 0.7); // Menggunakan rentang 0.0 - 1.0 untuk MediaPlayer volume
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);

        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(val.doubleValue());
            }
        });

        section.getChildren().addAll(volIcon, volumeSlider);
        return section;
    }

    private VBox buildUpNextSection() {
        VBox section = new VBox(5);
        Label lbl = new Label("UP NEXT");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.3);");
        section.getChildren().add(lbl);
        return section;
    }

    private HBox buildPlayerControls() {
        HBox controls = new HBox(30);
        controls.setPadding(new Insets(15, 30, 20, 30));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3a transparent transparent transparent;");

        Label prevBtn = createControlBtn("⏮", false);
        playBtn = new Label("▶");
        playBtn.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #a855f7; -fx-background-radius: 50; -fx-padding: 10 15 10 15; -fx-cursor: hand;");
        Label nextBtn = createControlBtn("⏭", false);

        playBtn.setOnMouseClicked(e -> togglePlay());
        prevBtn.setOnMouseClicked(e -> handlePrev());
        nextBtn.setOnMouseClicked(e -> handleNext());

        controls.getChildren().addAll(prevBtn, playBtn, nextBtn);
        return controls;
    }

    private Label createControlBtn(String icon, boolean active) {
        Label btn = new Label(icon);
        btn.setStyle("-fx-font-size: 20px; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand;");
        return btn;
    }

    // ============================================================
    // FIXED PLAYER LOGIC (MENGGUNAKAN JAVAFX MEDIA & TIMELINE EFISIEN)
    // ============================================================
    private void playSong(Song song) {
        // Matikan lagu sebelumnya
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose(); 
        }

        currentSong = song;
        currentIndex = allSongs.indexOf(song);
        isPlaying = true;
        
        // Update UI Kanan
        nowPlayingTitle.setText(song.getTitle());
        nowPlayingArtist.setText(song.getArtist());
        playBtn.setText("⏸");
        
        refreshSongList();

        try {
            // Ambil URL Audio
            String audioUrl = song.getFileUrl();
            if (audioUrl == null || audioUrl.isEmpty()) {
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"; 
            }
            
            Media media = new Media(audioUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.7); // Set volume awal
            
            // ==========================================
            // KUNCI SINKRONISASI WAKTU & PROGRESS BAR
            // ==========================================
            
            // 1. Update bar putih dan teks waktu (Kiri) tiap detik
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                javafx.application.Platform.runLater(() -> {
                    if (mediaPlayer.getTotalDuration() != null) {
                        double current = newTime.toSeconds();
                        double total = mediaPlayer.getTotalDuration().toSeconds();
                        
                        if (total > 0) {
                            // Gerakin bar putih
                            seekBar.setProgress(current / total);
                            
                            // Ganti teks detik berjalan (misal 1:24)
                            int min = (int) current / 60;
                            int sec = (int) current % 60;
                            currentTimeLabel.setText(String.format("%d:%02d", min, sec));
                        }
                    }
                });
            });

            // 2. Timpa tulisan 2:37 jadi durasi ASLI dari file MP3-nya (Kanan)
            mediaPlayer.setOnReady(() -> {
                javafx.application.Platform.runLater(() -> {
                    double total = mediaPlayer.getTotalDuration().toSeconds();
                    int totalMin = (int) total / 60;
                    int totalSec = (int) total % 60;
                    totalTimeLabel.setText(String.format("%d:%02d", totalMin, totalSec));
                });
            });
            // ==========================================

            mediaPlayer.play();
            
            // Otomatis lanjut lagu kalau habis
            mediaPlayer.setOnEndOfMedia(() -> handleNext());

        } catch (Exception e) {
            System.err.println("Gagal memutar audio: " + e.getMessage());
        }
    }
    
    // FUNGSI BUAT TOMBOL NEXT
    private void handleNext() {
        if (allSongs.isEmpty()) return;
        currentIndex = (currentIndex + 1) % allSongs.size();
        playSong(allSongs.get(currentIndex));
    }

    // COPAS FUNGSI INI DI BAWAH FUNGSI playSong ATAU handleNext
    private void togglePlay() {
        // Kalau belum milih lagu tapi udah mencet play, putar lagu pertama
        if (currentSong == null && !allSongs.isEmpty()) {
            playSong(allSongs.get(0));
            return;
        }

        // Kalau media player belum siap, jangan ngapa-ngapain
        if (mediaPlayer == null) return;

        isPlaying = !isPlaying;
        if (isPlaying) {
            mediaPlayer.play(); // Lanjutin muter
            playBtn.setText("⏸");
        } else {
            mediaPlayer.pause(); // Jeda lagu
            playBtn.setText("▶");
        }
    }

    private void handlePrev() {
        if (allSongs.isEmpty()) return;
        currentIndex = (currentIndex - 1 + allSongs.size()) % allSongs.size();
        playSong(allSongs.get(currentIndex));
    }

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
        st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        return st;
    }
}