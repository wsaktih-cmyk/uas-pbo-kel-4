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

/**
 * Kelompok 5: Master Central Controller (Pusat Kendali Aplikasi).
 * Kelas ini bertindak sebagai otak arsitektur aplikasi (Mediator Pattern) yang memegang 
 * reference seluruh komponen UI, sub-manager modular, data state lagu, serta memicu inisialisasi awal UI Melodify.
 */
public class PlaylistController {

    // Komponen Window Utama JavaFX
    Stage stage;

    // Koleksi data list reaktif (ObservableList) yang mendeteksi perubahan data untuk sinkronisasi otomatis ke UI
    ObservableList<Song> allSongs = FXCollections.observableArrayList();      // Master data seluruh lagu
    ObservableList<Song> filteredSongs = FXCollections.observableArrayList(); // Data lagu ter-filter (Home/Discover/Playlist)

    // State pemutaran yang dipantau secara global
    Song currentSong;
    int currentIndex = 0;
    boolean isPlaying = false;

    // Elemen Komponen UI Kontrol & Informasi Lagu
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

    // Komponen Manajemen Playlist Kustom buatan User
    VBox customPlaylistContainer;
    Map<String, ObservableList<Song>> playlistMap = new HashMap<>(); // Key: Nama Playlist, Value: Daftar kumpulan lagunya
    ComboBox<String> playlistSelector = new ComboBox<>();            // Dropdown menu untuk memilih playlist
    MediaPlayer mediaPlayer;                                         // Mesin pemutar media audio aktif

    // Deklarasi Sub-Manajer Modular yang terbagi sesuai tanggung jawab tugasnya (Separation of Concerns)
    DatabaseManager databaseManager;
    SidebarBuilder sidebarBuilder;
    SongListBuilder songListBuilder;
    PlayerManager playerManager;
    DialogManager dialogManager;
    UIComponentFactory uiFactory;

    /**
     * Method inisialisasi utama (Entry Point) untuk merakit dan menampilkan panggung utama program (Stage).
     * @param primaryStage Stage utama kiriman dari Main application class
     */
    public void initAndShow(Stage primaryStage) {
        this.stage = primaryStage;

        // Proses Instansiasi & Dependency Injection (memasukkan objek 'this' controller ke sub-manager)
        databaseManager = new DatabaseManager(this);
        sidebarBuilder = new SidebarBuilder(this);
        songListBuilder = new SongListBuilder(this);
        playerManager = new PlayerManager(this);
        dialogManager = new DialogManager(this);
        uiFactory = new UIComponentFactory(this);

        // Langsung hubungi database cloud Supabase untuk memuat data awal lagu sesaat setelah dibuka
        databaseManager.loadSongsFromSupabase();

        // Merakit seluruh struktur visual layout aplikasi ke dalam kontainer BorderPane utama
        BorderPane root = buildMainUI();

        // Menyiapkan Wadah Scene berukuran lebar 1280px dan tinggi 680px
        Scene scene = new Scene(root, 1280, 680);
        scene.setFill(Color.TRANSPARENT); // Set background transparan agar lengkungan radius window terlihat rapi

        // Mengatur properti konfigurasi Stage Window aplikasi
        stage.setTitle("🎵 Melodify - Music Player");
        stage.initStyle(StageStyle.UNDECORATED); // Menghilangkan frame bar bawaan Windows/Mac agar bisa dikustomisasi penuh
        stage.setScene(scene);
        stage.show(); // Tampilkan window ke monitor layar komputer
        stage.centerOnScreen(); // Atur posisi window otomatis pas ditengah-tengah layar

        // Menjalankan efek transisi halus pembukaan & mengaktifkan penarikan window via mouse drag
        AnimationManager.playEntryAnimation(root);
        AnimationManager.enableDrag(root, stage);
    }

    /**
     * Menyusun cetak biru tata letak struktur aplikasi (BorderPane layouting).
     * Membagi area tampilan ke dalam 5 sektor utama (Top, Left, Center, Right, Bottom).
     */
    private BorderPane buildMainUI() {
        BorderPane root = new BorderPane();
        // Pewarnaan warna tema gelap (navy deep) dan melengkungkan ujung pinggiran aplikasi sebesar 20px
        root.setStyle("-fx-background-color: #0d0d1a; -fx-background-radius: 20;");

        root.setTop(buildTitleBar());                        // Bagian Atas: Judul aplikasi & tombol Windows (min/max/close)
        root.setLeft(sidebarBuilder.buildSidebar());          // Bagian Kiri: Menu Navigasi & Daftar Playlist Kustom
        root.setCenter(songListBuilder.buildPlaylistSection()); // Bagian Tengah: Daftar Baris Lagu Utama (Konten Inti)
        root.setRight(buildNowPlayingPanel());                // Bagian Kanan: Panel Informasi Lagu yang sedang berputar + Album Art
        root.setBottom(uiFactory.buildPlayerControls());      // Bagian Bawah: Bar Tombol Controller Play/Next/Prev & Volume Slider

        return root;
    }

    /**
     * Membangun Bar Judul Kustom (Title Bar Atas) pengganti bawaan OS Windows/Mac yang dihilangkan.
     */
    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(15, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);

        // Komponen teks Logo Brand Aplikasi
        Label logo = new Label("🎵 MELODIFY");
        logo.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");

        // Memberikan efek pendaran cahaya neon (DropShadow Glow) berwarna ungu mistis pada teks Logo
        DropShadow logoGlow = new DropShadow();
        logoGlow.setRadius(15);
        logoGlow.setColor(Color.web("#a855f7"));
        logo.setEffect(logoGlow);

        // Region Spacer elastis: Mendorong tombol navigasi window ke sudut paling kanan ujung
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Membuat 3 buah tombol bulat tiruan jendela Windows (Minimize, Maximize, Close) beserta warnanya
        Label min = uiFactory.createWindowBtn("—", "#f59e0b");
        Label max = uiFactory.createWindowBtn("⬜", "#22c55e");
        Label close = uiFactory.createWindowBtn("✕", "#ef4444");

        // Menghubungkan fungsi aksi event klik mouse pada masing-masing tombol jendela
        min.setOnMouseClicked(e -> stage.setIconified(true)); // Perkecil ke Taskbar
        max.setOnMouseClicked(e -> stage.setMaximized(!stage.isMaximized())); // Fullscreen toggle
        close.setOnMouseClicked(e -> {
            // Berhenti memutar audio terlebih dahulu sebelum menutup aplikasi demi kenyamanan sound driver OS
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            stage.close(); // Tutup aplikasi sepenuhnya
        });

        HBox controls = new HBox(8); // Satukan ketiga tombol dengan celah spasi 8px
        controls.getChildren().addAll(min, max, close);

        bar.getChildren().addAll(logo, spacer, controls);
        return bar;
    }

    /**
     * Merancang struktur visual Panel Samping Kanan (Now Playing Dashboard Display).
     */
    private VBox buildNowPlayingPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(260);
        panel.setPadding(new Insets(10, 15, 10, 15));
        // Pewarnaan warna panel lebih gelap dari background utama, diberi pembatas border vertical tipis di sebelah kiri
        panel.setStyle("-fx-background-color: #111127; -fx-border-color: transparent transparent transparent #1e1e3a; -fx-border-width: 0 0 0 1;");

        Label pTitle = new Label("NOW PLAYING");
        pTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.3);");

        // Inisialisasi teks judul lagu, jika belum ada lagu terpasang maka default menampilkan pesan teks "No Song"
        nowPlayingTitle = new Label(currentSong != null ? currentSong.getTitle() : "No Song");
        nowPlayingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");

        nowPlayingArtist = new Label(currentSong != null ? currentSong.getArtist() : "Unknown Artist");
        nowPlayingArtist.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");

        // Menyusun bertumpuk vertikal ke bawah seluruh panel pelengkap informasi lagu yang diproduksi oleh uiFactory
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