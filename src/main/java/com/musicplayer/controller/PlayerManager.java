package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class PlayerManager {

    private final PlaylistController controller;

    public PlayerManager(PlaylistController controller) {
        this.controller = controller;
    }

    public void playSong(Song song) {
        if (controller.mediaPlayer != null) {
            controller.mediaPlayer.stop();
            controller.mediaPlayer.dispose();
        }

        controller.currentSong = song;
        controller.currentIndex = controller.allSongs.indexOf(song);
        controller.isPlaying = true;
        controller.nowPlayingTitle.setText(song.getTitle());
        controller.nowPlayingArtist.setText(song.getArtist());
        controller.playBtn.setText("⏸");
        controller.songListBuilder.refreshSongList();

        try {
            String audioUrl = song.getFileUrl();
            if (audioUrl == null || audioUrl.isEmpty()) {
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
            }

            audioUrl = audioUrl.replace(" ", "%20");

            Media media = new Media(audioUrl);
            controller.mediaPlayer = new MediaPlayer(media);
            controller.mediaPlayer.setVolume(0.7);

            controller.mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                Platform.runLater(() -> {
                    if (controller.mediaPlayer.getTotalDuration() != null) {
                        double current = newTime.toSeconds();
                        double total = controller.mediaPlayer.getTotalDuration().toSeconds();
                        if (total > 0) {
                            controller.seekBar.setProgress(current / total);
                            int min = (int) current / 60;
                            int sec = (int) current % 60;
                            controller.currentTimeLabel.setText(String.format("%d:%02d", min, sec));
                        }
                    }
                });
            });

            controller.mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    double total = controller.mediaPlayer.getTotalDuration().toSeconds();
                    int totalMin = (int) total / 60;
                    int totalSec = (int) total % 60;
                    String realDuration = String.format("%d:%02d", totalMin, totalSec);

                    controller.totalTimeLabel.setText(realDuration);

                    if (controller.currentSong != null) {
                        controller.currentSong.setDuration(realDuration);
                        controller.songListBuilder.refreshSongList();
                    }
                });
            });

            controller.mediaPlayer.play();
            controller.mediaPlayer.setOnEndOfMedia(this::handleNext);
        } catch (Exception e) {
            System.err.println("Gagal memutar audio: " + e.getMessage());
        }
    }

    public void togglePlay() {
        if (controller.allSongs.isEmpty()) {
            return;
        }

        if (controller.mediaPlayer == null) {
            if (controller.currentSong == null) {
                playSong(controller.allSongs.get(0));
            } else {
                playSong(controller.currentSong);
            }
            return;
        }

        controller.isPlaying = !controller.isPlaying;
        if (controller.isPlaying) {
            controller.mediaPlayer.play();
            controller.playBtn.setText("⏸");
        } else {
            controller.mediaPlayer.pause();
            controller.playBtn.setText("▶");
        }
    }

    public void handleNext() {
        if (!controller.allSongs.isEmpty()) {
            controller.currentIndex = (controller.currentIndex + 1) % controller.allSongs.size();
            playSong(controller.allSongs.get(controller.currentIndex));
        }
    }

    public void handlePrev() {
        if (!controller.allSongs.isEmpty()) {
            controller.currentIndex = (controller.currentIndex - 1 + controller.allSongs.size()) % controller.allSongs.size();
            playSong(controller.allSongs.get(controller.currentIndex));
        }
    }
}