package com.musicplayer.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.musicplayer.model.Song;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

public class DatabaseManager {

    private final PlaylistController controller;

    public DatabaseManager(PlaylistController controller) {
        this.controller = controller;
    }

    public void loadSongsFromSupabase() {
        String sql = "SELECT * FROM songs ORDER BY id ASC";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            controller.allSongs.clear();
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
                controller.allSongs.add(song);
            }
            controller.filteredSongs.setAll(controller.allSongs);

            if (!controller.allSongs.isEmpty()) {
                controller.currentSong = controller.allSongs.get(0);
            }
        } catch (Exception e) {
            System.err.println("Gagal sinkronisasi dengan Supabase Cloud.");
            e.printStackTrace();
        }
    }

    public void saveSongToSupabase(Song song) {
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

            controller.allSongs.add(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setHeaderText(null);
            success.setContentText("Lagu berhasil dikirim ke Supabase!");
            success.show();
        } catch (Exception e) {
            controller.allSongs.add(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList();

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Gagal simpan ke cloud, lagu disimpan lokal sementara.\nError: " + e.getMessage());
            alert.show();
        }
    }

    public void deleteSongFromSupabase(Song song) {
        String sql = "DELETE FROM songs WHERE title = ? AND artist = ?";
        try (Connection conn = com.musicplayer.model.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.executeUpdate();

            controller.allSongs.remove(song);
            controller.filteredSongs.setAll(controller.allSongs);

            for (ObservableList<Song> pl : controller.playlistMap.values()) {
                pl.remove(song);
            }
            controller.songListBuilder.refreshSongList();

            if (controller.currentSong == song) {
                if (controller.mediaPlayer != null) {
                    controller.mediaPlayer.stop();
                    controller.mediaPlayer.dispose();
                    controller.mediaPlayer = null;
                }
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
            controller.allSongs.remove(song);
            controller.filteredSongs.setAll(controller.allSongs);
            controller.songListBuilder.refreshSongList();
        }
    }
}