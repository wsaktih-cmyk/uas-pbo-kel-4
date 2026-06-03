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

/**
 * [PENJELASAN]: Kelas ini berfungsi untuk membangun struktur visual Sidebar (menu samping kiri).
 * Menggunakan arsitektur berbasis komponen untuk memisahkan UI layout dari Main Controller.
 */
public class SidebarBuilder {

    // [PENJELASAN]: Reference ke PlaylistController utama agar bisa memanipulasi state global aplikasi
    private final PlaylistController controller;

    /**
     * [PENJELASAN]: Konstruktor untuk menyuntikkan (inject) instance dari PlaylistController.
     */
    public SidebarBuilder(PlaylistController controller) {
        this.controller = controller;
    }

    /**
     * [PENJELASAN]: Method utama untuk merakit susunan komponen penampung sidebar kiri.
     * @return VBox yang sudah berisi menu navigasi, list playlist, dan profil user.
     */
    public VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(10, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #111127; -fx-border-color: transparent #1e1e3a transparent transparent; -fx-border-width: 0 1 0 0;");

        // [PENJELASAN]: Memasukkan 3 item navigasi statis bawaan aplikasi ke dalam sidebar
        sidebar.getChildren().add(createSidebarItem("🏠", "Home", true));
        sidebar.getChildren().add(createSidebarItem("🔍", "Discover", false));
        sidebar.getChildren().add(createSidebarItem("💖", "Liked Songs", false));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a;");

        // [PENJELASAN]: Menyusun struktur layout header teks "MY PLAYLISTS" beserta tombol tambah "➕"
        HBox pHeaderBox = new HBox();
        pHeaderBox.setAlignment(Pos.CENTER_LEFT);
        pHeaderBox.setPadding(new Insets(15, 5, 5, 5));

        Label pHeader = new Label("MY PLAYLISTS");
        pHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS); // Mendorong tombol plus ke ujung paling kanan

        Label addPBtn = new Label("➕");
        addPBtn.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-cursor: hand;");
        addPBtn.setOnMouseClicked(e -> handleCreatePlaylist());

        pHeaderBox.getChildren().addAll(pHeader, sp, addPBtn);

        // [PENJELASAN]: Mengaitkan container dinamis di controller untuk menampung item playlist buatan user
        controller.customPlaylistContainer = new VBox(5);
        sidebar.getChildren().addAll(sep, pHeaderBox, controller.customPlaylistContainer);

        // [PENJELASAN]: Spacer elastis untuk memaksa widget profil berada di posisi paling dasar sidebar
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, createUserProfile());

        return sidebar;
    }

    /**
     * [PENJELASAN]: Mengatur event pop-up dialog saat user ingin membuat playlist baru.
     */
    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Create New Playlist");
        dialog.setHeaderText("Bikin playlist kustom baru lu bre");
        dialog.setContentText("Masukkan nama playlist:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String pName = name.trim();
            // [PENJELASAN]: Validasi agar tidak ada nama playlist kosong atau nama ganda yang terduplikasi
            if (!pName.isEmpty() && !controller.playlistMap.containsKey(pName)) {
                controller.playlistMap.put(pName, FXCollections.observableArrayList());
                controller.playlistSelector.getItems().add(pName); // Update combobox di panel lain
                controller.customPlaylistContainer.getChildren().add(createPlaylistSidebarItem("🎵", pName, "0 songs"));
            }
        });
    }

    /**
     * [PENJELASAN]: Pabrik pembuat baris item menu utama (Home, Discover, Liked).
     * Di dalamnya terdapat algoritma untuk mereset fokus visual menu tetangga saat diklik.
     */
    private HBox createSidebarItem(String icon, String text, boolean isInitiallyActive) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (isInitiallyActive ? "bold" : "normal") + "; -fx-text-fill: " + (isInitiallyActive ? "white" : "rgba(255,255,255,0.6)") + ";");

        // [PENJELASAN]: Bar indikator vertikal gradasi warna ungu-cyan di sisi kiri menu aktif
        Rectangle indicator = new Rectangle(3, 20);
        indicator.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a855f7")),
                new Stop(1, Color.web("#06b6d4"))));
        indicator.setVisible(isInitiallyActive);

        if (isInitiallyActive) {
            item.setStyle("-fx-background-color: linear-gradient(to right, #a855f720, #06b6d410); -fx-background-radius: 12;");
        }

        item.getChildren().addAll(indicator, iconLabel, textLabel);

        // [PENJELASAN]: Logika filtering data lagu di area tengah berdasarkan menu yang dipilih
        item.setOnMouseClicked(e -> {
            switch (text) {
                case "Home":
                    controller.sectionTitleLabel.setText("Home");
                    controller.sectionSubtitleLabel.setText("Welcome back! Ready to listen to some music?");
                    controller.addSongBtn.setVisible(true);
                    controller.filteredSongs.setAll(controller.allSongs); // Tampilkan semua lagu
                    break;
                case "Discover":
                    controller.sectionTitleLabel.setText("Discover");
                    controller.sectionSubtitleLabel.setText("Rekomendasi hits viral buat lu hari ini 🔥");
                    controller.addSongBtn.setVisible(false);
                    controller.filteredSongs.clear();
                    controller.filteredSongs.addAll(controller.allSongs);
                    java.util.Collections.shuffle(controller.filteredSongs); // Fitur acak lagu otomatis
                    break;
                case "Liked Songs":
                    controller.sectionTitleLabel.setText("Liked Songs");
                    controller.sectionSubtitleLabel.setText("Your absolute favorites 💖");
                    controller.addSongBtn.setVisible(false);
                    controller.filteredSongs.clear();
                    // [PENJELASAN]: Memanfaatkan Stream API untuk memfilter lagu yang ditandai Like saja
                    controller.allSongs.stream().filter(Song::isLiked).forEach(controller.filteredSongs::add);
                    break;
            }
            controller.songListBuilder.refreshSongList();

            // [PENJELASAN]: Reset gaya CSS seluruh menu tetangga agar status "aktif" tidak tumpang tindih
            VBox parent = (VBox) item.getParent();
            for (Node node : parent.getChildren()) {
                if (node instanceof HBox) {
                    HBox sibling = (HBox) node;
                    if (!sibling.getChildren().isEmpty() && sibling.getChildren().get(0) instanceof Rectangle) {
                        Rectangle ind = (Rectangle) sibling.getChildren().get(0);
                        Label lbl = (Label) sibling.getChildren().get(2);
                        ind.setVisible(false);
                        sibling.setStyle("-fx-background-color: transparent;");
                        lbl.setStyle