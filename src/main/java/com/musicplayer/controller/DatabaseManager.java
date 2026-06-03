package com.musicplayer.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.musicplayer.model.Song;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

/**
 * Kelompok 2: Manajemen Basis Data Cloud (Supabase).
 * Class ini menangani seluruh operasi CRUD (Create, Read, Delete) data lagu
 * antara aplikasi JavaFX dengan database Supabase PostgreSQL menggunakan JDBC.
 */
public class DatabaseManager {

    // Reference ke core controller untuk mengubah state UI secara langsung
    private final PlaylistController controller;

    /**
     * Konstruktor untuk menghubungkan DatabaseManager dengan PlaylistController utama.
     */
    public DatabaseManager(PlaylistController controller) {
        this.controller = controller;
    }

    /**
     * Mengambil (Fetch) seluruh data lagu yang tersimpan di database Supabase Cloud.
     */
    public void loadSongsFromSupabase() {
        String sql = "SELECT * FROM songs ORDER BY id ASC";
        
        // Membuka koneksi database dan mempersiapkan query statement (Try-with-resources otomatis menutup koneksi)
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // Bersihkan list lagu lama di UI agar tidak terjadi duplikasi data
            controller.allSongs.clear();
            
            // Looping mengambil data baris per baris hasil query
            while (rs.next()) {
                // Instansiasi objek Song baru berdasarkan data dari database
                Song song = new Song(
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("album"),
                        rs.getString("duration"),
                        rs.getString("genre"),
                        rs.getString("cover_emoji")
                );
                song.setFileUrl(rs.getString("file_url")); // Set link streaming MP3
                controller.allSongs.add(song); // Masukkan ke master list data
            }
            // Sinkronisasi data list yang difilter dengan master list lagu terbaru
            controller.filteredSongs.setAll(controller.allSongs);

            // Default: Jika ada lagu yang termuat, pasang lagu indeks ke-0 sebagai lagu aktif ready-to-play
            if (!controller.allSongs.isEmpty()) {
                controller.currentSong = controller.allSongs.get(0);
            }
        } catch (Exception e) {
            System.err.println("Gagal sinkronisasi dengan Supabase Cloud.");
            e.printStackTrace();
        }
    }

    /**
     * Menyimpan (Insert) data lagu baru yang diinput user melalui Dialog ke database Supabase.
     * @param song Objek lagu baru yang siap dikirim
     */
    public void saveSongToSupabase(Song song) {
        String sql = "INSERT INTO songs (title, artist, album, duration, genre, cover_emoji, file_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Mapping data model Song ke dalam parameter query (?) tanda tanya mencegah SQL Injection
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setString(4, song.getDuration());
            pstmt.setString(5, song.getGenre());
            pstmt.setString(6, song.getCoverEmoji());
            pstmt.setString(7, song.getFileUrl());
            pstmt.executeUpdate(); // Eksekusi query insert ke cloud

            // Perbarui UI secara lokal setelah data sukses terkirim ke database cloud
            controller.allSongs.add(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList(); // Re-render tampilan list lagu

            // Tampilkan pop-up pemberitahuan sukses
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setHeaderText(null);
            success.setContentText("Lagu berhasil dikirim ke Supabase!");
            success.show();
        } catch (Exception e) {
            // Mekanisme Fallback: Jika koneksi cloud gagal/error, lagu tetap dimasukkan ke memori lokal sementara
            controller.allSongs.add(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList();

            // Tampilkan pop-up peringatan bahwa data gagal tersimpan di cloud
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Gagal simpan ke cloud, lagu disimpan lokal sementara.\nError: " + e.getMessage());
            alert.show();
        }
    }

    /**
     * Menghapus (Delete) data lagu dari database Supabase berdasarkan kombinasi judul dan artis.
     * @param song Objek lagu yang ingin dihapus
     */
    public void deleteSongFromSupabase(Song song) {
        String sql = "DELETE FROM songs WHERE title = ? AND artist = ?";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.executeUpdate(); // Jalankan penghapusan di database cloud

            // Hapus lagu dari list utama di memori aplikasi
            controller.allSongs.remove(song);
            controller.filteredSongs.setAll(controller.allSongs);

            // Cari dan bersihkan data lagu ini dari seluruh playlist kustom bikinan user (jika terdaftar di dalamnya)
            for (ObservableList<Song> pl : controller.playlistMap.values()) {
                pl.remove(song);
            }
            controller.songListBuilder.refreshSongList(); // Re-render visual list lagu

            // Validasi: Jika lagu yang dihapus kebetulan sedang diputar sekarang, maka matikan paksa player-nya
            if (controller.currentSong == song) {
                if (controller.mediaPlayer != null) {
                    controller.mediaPlayer.stop();
                    controller.mediaPlayer.dispose(); // Bersihkan resource player dari memori
                    controller.mediaPlayer = null;
                }
                // Reset seluruh komponen UI panel 'Now Playing' kembali ke state kosong
                controller.currentSong = null;
                controller.isPlaying = false;
                controller.nowPlayingTitle.setText("No Song");
                controller.nowPlayingArtist.setText("Unknown Artist");
                controller.playBtn.setText("▶");
                controller.currentTimeLabel.setText("0:00");
                controller.totalTimeLabel.setText("0:00");
                controller.seekBar.setProgress(0);
            }
        } catch (Exception e) {
            // Fallback apabila koneksi bermasalah: Tetap hapus dari memori UI agar user tidak bingung
            controller.allSongs.remove(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList();
        }
    }
}