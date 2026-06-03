package com.musicplayer.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Kelompok 1: Manajemen Animasi & Interaksi Window.
 * Class ini bertanggung jawab penuh untuk mengatur efek visual transisi 
 * serta fitur dragging window karena aplikasi menggunakan style UNDECORATED (tanpa frame bawaan OS).
 */
public class AnimationManager {

    // Menyimpan posisi koordinat kursor mouse relatif terhadap Scene saat pertama kali diklik
    private static double xOffset = 0;
    private static double yOffset = 0;

    /**
     * Mengaktifkan fitur geser (drag) pada window aplikasi.
     * @param root Node utama tempat mendengarkan event mouse
     * @param stage Stage JavaFX yang akan dipindahkan posisinya
     */
    public static void enableDrag(Node root, Stage stage) {
        // Event saat mouse pertama kali ditekan: Ambil koordinat titik klik
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        // Event saat mouse diseret/digeser
        root.setOnMouseDragged(e -> {
            // Batasan 'yOffset < 60' memastikan jendela hanya bisa didrag 
            // jika pengguna mengklik area title bar atas (lebar maksimal 60 pixel)
            if (yOffset < 60) {
                // Atur posisi koordinat stage baru berdasarkan pergerakan mouse di layar
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    /**
     * Memainkan animasi smooth ketika aplikasi pertama kali dibuka (Entry Animation).
     * Menggabungkan efek memudar (Fade) dan pembesaran ukuran (Scale).
     * @param root Node target yang akan dianimasikan
     */
    public static void playEntryAnimation(Node root) {
        // State awal sebelum animasi jalan: Transparan dan sedikit mengecil (95% dari ukuran asli)
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);

        // 1. Membuat animasi transparansi (Fade) menuju nilai 1 (100% muncul) berdurasi 500ms
        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setToValue(1);

        // 2. Membuat animasi ukuran (Scale) menuju ukuran asli (1.0) berdurasi 500ms
        ScaleTransition st = new ScaleTransition(Duration.millis(500), root);
        st.setToX(1.0);
        st.setToY(1.0);

        // 3. Menggabungkan kedua animasi diatas agar berjalan berbarengan secara serentak
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play(); // Jalankan animasi
    }
}