package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Kelompok 4: Manajemen Pemutaran Audio (Audio Core Engine).
 * Kelas krusial yang mengontrol siklus hidup media player JavaFX (play, pause, next, prev, 
 * perhitungan durasi progress bar, serta auto-play lagu selanjutnya).
 */
public class PlayerManager {

    private final PlaylistController controller;

    /**
     * Konstruktor PlayerManager.
     */
    public PlayerManager(PlaylistController controller) {
        this.controller = controller;
    }

    /**
     * Menginisialisasi komponen Media dan menyetel pemutaran audio file lagu terpilih.
     * @param song Objek lagu yang ingin diputar saat ini
     */
    public void playSong(Song song) {
        // Melakukan pembersihan (clean up) jika ada lagu lain yang sedang berjalan sebelumnya
        if (controller.mediaPlayer != null) {
            controller.mediaPlayer.stop();
            controller.mediaPlayer.dispose(); // Membebaskan native resources sistem operasi agar tidak bocor (memory leak)
        }

        // Sinkronisasi status/state kontroler aplikasi dengan data lagu aktif saat ini
        controller.currentSong = song;
        controller.currentIndex = controller.allSongs.indexOf(song);
        controller.isPlaying = true;
        controller.nowPlayingTitle.setText(song.getTitle());
        controller.nowPlayingArtist.setText(song.getArtist());
        controller.playBtn.setText("⏸"); // Ubah ikon tombol ke simbol Pause
        controller.songListBuilder.refreshSongList(); // Refresh list agar baris lagu aktif berubah warna/style

        try {
            String audioUrl = song.getFileUrl();
            // Validasi: Jika tautan file kosong/rusak, dialihkan ke file MP3 dummy online sebagai sample backup
            if (audioUrl == null || audioUrl.isEmpty()) {
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
            }

            // URL Encoding manual: Mengganti karakter spasi mentah menjadi '%20' agar valid dibaca oleh class Media network
            audioUrl = audioUrl.replace(" ", "%20");

            Media media = new Media(audioUrl); // Load data stream audio internet/lokal
            controller.mediaPlayer = new MediaPlayer(media); // Pasang engine player baru
            controller.mediaPlayer.setVolume(0.7); // Mengatur tingkat kekerasan suara default (70%)

            // [Listener 1] Mendeteksi pergeseran/perubahan waktu putar berjalan secara Real-time (Time Tracking)
            controller.mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                // Platform.runLater mengantrekan perubahan UI ke JavaFX Application Thread agar thread-safe dan tidak lag
                Platform.runLater(() -> {
                    if (controller.mediaPlayer.getTotalDuration() != null) {
                        double current = newTime.toSeconds();
                        double total = controller.mediaPlayer.getTotalDuration().toSeconds();
                        if (total > 0) {
                            // Menghitung persentase rasio (0.0 hingga 1.0) untuk memperbarui progress isi SeekBar
                            controller.seekBar.setProgress(current / total);
                            
                            // Konversi hitungan total detik berjalan ke format string menit:detik waktu berjalan (e.g., 3:05)
                            int min = (int) current / 60;
                            int sec = (int) current % 60;
                            controller.currentTimeLabel.setText(String.format("%d:%02d", min, sec));
                        }
                    }
                });
            });

            // [Listener 2] Terpicu saat file audio selesai di-buffer dan sistem siap memutar (Ready State)
            controller.mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    // Mengambil total durasi asli dari file lagu secara akurat
                    double total = controller.mediaPlayer.getTotalDuration().toSeconds();
                    int totalMin = (int) total / 60;
                    int totalSec = (int) total % 60;
                    String realDuration = String.format("%d:%02d", totalMin, totalSec);

                    // Tampilkan durasi real tersebut ke label pojok kanan seekbar UI
                    controller.totalTimeLabel.setText(realDuration);

                    // Update metadata durasi asli objek lagu di dalam aplikasi agar sinkron
                    if (controller.currentSong != null) {
                        controller.currentSong.setDuration(realDuration);
                        controller.songListBuilder.refreshSongList();
                    }
                });
            });

            // Mulai putar musik
            controller.mediaPlayer.play();
            // [Listener 3] Jika musik habis selesai diputar, otomatis panggil fungsi handleNext untuk lagu selanjutnya
            controller.mediaPlayer.setOnEndOfMedia(this::handleNext);
        } catch (Exception e) {
            System.err.println("Gagal memutar audio: " + e.getMessage());
        }
    }

    /**
     * Aksi Tombol Play/Pause. Menangani toggle kondisi pemutaran musik saat ini.
     */
    public void togglePlay() {
        if (controller.allSongs.isEmpty()) {
            return; // Batalkan jika tidak ada daftar lagu sama sekali
        }

        // Jika player belum diinisiasi (keadaan baru buka aplikasi), putar lagu perdana
        if (controller.mediaPlayer == null) {
            if (controller.currentSong == null) {
                playSong(controller.allSongs.get(0));
            } else {
                playSong(controller.currentSong);
            }
            return;
        }

        // Mengubah status boolean ke kebalikannya (True jadi False / False jadi True)
        controller.isPlaying = !controller.isPlaying;
        if (controller.isPlaying) {
            controller.mediaPlayer.play(); // Lanjutkan musik
            controller.playBtn.setText("⏸");
        } else {
            controller.mediaPlayer.pause(); // Jeda musik
            controller.playBtn.setText("▶");
        }
    }

    /**
     * Menggeser pemutaran musik melompat maju ke lagu berikutnya (Next Track).
     */
    public void handleNext() {
        if (!controller.allSongs.isEmpty()) {
            // Rumus modulo (%) memastikan jika index sudah mencapai ujung akhir list, ia berputar kembali ke index 0
            controller.currentIndex = (controller.currentIndex + 1) % controller.allSongs.size();
            playSong(controller.allSongs.get(controller.currentIndex));
        }
    }

    /**
     * Menggeser pemutaran musik mundur kembali ke lagu sebelumnya (Previous Track).
     */
    public void handlePrev() {
        if (!controller.allSongs.isEmpty()) {
            // Ditambah size() terlebih dahulu sebelum dimodulo untuk mencegah hasil kalkulasi bernilai negatif
            controller.currentIndex = (controller.currentIndex - 1 + controller.allSongs.size()) % controller.allSongs.size();
            playSong(controller.allSongs.get(controller.currentIndex));
        }
    }
}