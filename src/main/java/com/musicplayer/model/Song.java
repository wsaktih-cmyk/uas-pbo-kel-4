package com.musicplayer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Song {
    private final StringProperty title;
    private final StringProperty artist;
    private final StringProperty album;
    private final StringProperty duration;
    private final StringProperty genre;
    private final BooleanProperty liked;
    private final StringProperty coverEmoji;
    private String fileUrl;

    public String getFileUrl() { return fileUrl; }
public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Song(String title, String artist, String album, 
                String duration, String genre, String coverEmoji) {
        this.title = new SimpleStringProperty(title);
        this.artist = new SimpleStringProperty(artist);
        this.album = new SimpleStringProperty(album);
        this.duration = new SimpleStringProperty(duration);
        this.genre = new SimpleStringProperty(genre);
        this.liked = new SimpleBooleanProperty(false);
        this.coverEmoji = new SimpleStringProperty(coverEmoji);
    }

    // Getters & Property methods
    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getArtist() { return artist.get(); }
    public StringProperty artistProperty() { return artist; }

    public String getAlbum() { return album.get(); }
    public StringProperty albumProperty() { return album; }

    public String getDuration() { return duration.get(); }
    public StringProperty durationProperty() { return duration; }

    public String getGenre() { return genre.get(); }
    public StringProperty genreProperty() { return genre; }

    public boolean isLiked() { return liked.get(); }
    public BooleanProperty likedProperty() { return liked; }
    public void setLiked(boolean liked) { this.liked.set(liked); }

    public String getCoverEmoji() { return coverEmoji.get(); }
    public StringProperty coverEmojiProperty() { return coverEmoji; }
}
