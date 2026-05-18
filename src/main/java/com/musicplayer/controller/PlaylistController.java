package com.musicplayer.controller;

import java.util.HashMap;
import java.util.Map;

import com.musicplayer.model.Song;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PlaylistController {

    Stage stage;

    ObservableList<Song> allSongs = FXCollections.observableArrayList();
    ObservableList<Song> filteredSongs = FXCollections.observableArrayList();

    Song currentSong;
    int currentIndex = 0;
    boolean isPlaying = false;

    Label nowPlayingTitle;
    Label nowPlayingArtist;
    Label playBtn;
    ProgressBar seekBar;
    Label currentTimeLabel;
    Label totalTimeLabel;
    VBox songListContainer;
    Label sectionTitleLabel;
    Label sectionSubtitleLabel;
    Button addSongBtn;

    VBox customPlaylistContainer;
    Map<String, ObservableList<Song>> playlistMap = new HashMap<>();
    ComboBox<String> playlistSelector = new ComboBox<>();
    MediaPlayer mediaPlayer;

    DatabaseManager databaseManager;
    SidebarBuilder sidebarBuilder;
    SongListBuilder songListBuilder;
    PlayerManager playerManager;
    DialogManager dialogManager;
    UIComponentFactory uiFactory;

    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;

        databaseManager = new DatabaseManager(this);
        sidebarBuilder = new SidebarBuilder(this);
        songListBuilder = new SongListBuilder(this);
        playerManager = new PlayerManager(this);
        dialogManager = new DialogManager(this);
        uiFactory = new UIComponentFactory(this);

        databaseManager.loadSongsFromSupabase();

        BorderPane root = buildMainUI();

        Scene scene = new Scene(root, 1280, 680);
        scene.setFill(Color.TRANSPARENT);

        stage.setTitle("🎵 Melodify - Music Player");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();

        AnimationManager.playEntryAnimation(root);
        AnimationManager.enableDrag(root, stage);
    }

    private BorderPane buildMainUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0d1a; -fx-background-radius: 20;");

        root.setTop(buildTitleBar());
        root.setLeft(sidebarBuilder.buildSidebar());
        root.setCenter(songListBuilder.buildPlaylistSection());
        root.setRight(buildNowPlayingPanel());
        root.setBottom(uiFactory.buildPlayerControls());

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

        Label min = uiFactory.createWindowBtn("—", "#f59e0b");
        Label max = uiFactory.createWindowBtn("⬜", "#22c55e");
        Label close = uiFactory.createWindowBtn("✕", "#ef4444");

        min.setOnMouseClicked(e -> stage.setIconified(true));
        max.setOnMouseClicked(e -> stage.setMaximized(!stage.isMaximized()));
        close.setOnMouseClicked(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            stage.close();
        });

        HBox controls = new HBox(8);
        controls.getChildren().addAll(min, max, close);

        bar.getChildren().addAll(logo, spacer, controls);
        return bar;
    }

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

        panel.getChildren().addAll(
                pTitle,
                uiFactory.createAlbumArt(),
                nowPlayingTitle,
                nowPlayingArtist,
                uiFactory.buildProgressSection(),
                uiFactory.buildVolumeSection(),
                uiFactory.buildAddToPlaylistSection()
        );
        return panel;
    }
}