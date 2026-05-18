package com.musicplayer.controller;

import com.musicplayer.model.Song;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;

public class DialogManager {

    private final PlaylistController controller;

    public DialogManager(PlaylistController controller) {
        this.controller = controller;
    }

    public void openAddSongDialog() {
        Dialog<Song> dialog = new Dialog<>();
        dialog.setTitle("Tambah Lagu Baru");
        dialog.initStyle(StageStyle.UTILITY);

        ButtonType saveButtonType = new ButtonType("Save to Supabase", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField t = new TextField(); t.setPromptText("Judul");
        TextField a = new TextField(); a.setPromptText("Artis");
        TextField al = new TextField(); al.setPromptText("Album");
        TextField g = new TextField(); g.setPromptText("Genre");
        TextField d = new TextField(); d.setPromptText("Durasi");
        TextField e = new TextField(); e.setPromptText("Emoji");
        TextField u = new TextField(); u.setPromptText("URL MP3 (Link Supabase Storage)");

        grid.addRow(0, new Label("Judul:"), t);
        grid.addRow(1, new Label("Artis:"), a);
        grid.addRow(2, new Label("Album:"), al);
        grid.addRow(3, new Label("Genre:"), g);
        grid.addRow(4, new Label("Durasi:"), d);
        grid.addRow(5, new Label("Emoji:"), e);
        grid.addRow(6, new Label("URL MP3:"), u);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return new Song(t.getText(), a.getText(), al.getText(), d.getText(), g.getText(), e.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            s.setFileUrl(u.getText().trim());
            controller.databaseManager.saveSongToSupabase(s);
        });
    }
}