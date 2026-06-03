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

/**
 * Kelompok 3: Manajemen Jendela Dialog Modal.
 * Class ini menangani pembuatan form pop-up inputan (Dialog Box) untuk menambahkan
 * metadata lagu baru secara manual oleh user.
 */
public class DialogManager {

    private final PlaylistController controller;

    /**
     * Konstruktor DialogManager.
     */
    public DialogManager(PlaylistController controller) {
        this.controller = controller;
    }

    /**
     * Membangun, menyusun layout, dan menampilkan dialog form "Tambah Lagu Baru".
     */
    public void openAddSongDialog() {
        // Inisialisasi kontainer Dialog JavaFX bermodel tipe data kembalian objek 'Song'
        Dialog<Song> dialog = new Dialog<>();
        dialog.setTitle("Tambah Lagu Baru");
        dialog.initStyle(StageStyle.UTILITY); // Desain minimalis jendela utility tanpa tombol max/min

        // Membuat tombol aksi kustom untuk simpan data dan menyematkan tombol cancel bawaan
        ButtonType saveButtonType = new ButtonType("Save to Supabase", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Mengatur layout form menggunakan struktur grid (baris & kolom) beserta padding/jaraknya
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Membuat field input berupa kotak ketik teks (TextField) beserta placeholder-nya
        TextField t = new TextField(); t.setPromptText("Judul");
        TextField a = new TextField(); a.setPromptText("Artis");
        TextField al = new TextField(); al.setPromptText("Album");
        TextField g = new TextField(); g.setPromptText("Genre");
        TextField d = new TextField(); d.setPromptText("Durasi");
        TextField e = new TextField(); e.setPromptText("Emoji");
        TextField u = new TextField(); u.setPromptText("URL MP3 (Link Supabase Storage)");

        // Menyusun komponen Label (keterangan) dan TextField (input) ke dalam baris grid
        grid.addRow(0, new Label("Judul:"), t);
        grid.addRow(1, new Label("Artis:"), a);
        grid.addRow(2, new Label("Album:"), al);
        grid.addRow(3, new Label("Genre:"), g);
        grid.addRow(4, new Label("Durasi:"), d);
        grid.addRow(5, new Label("Emoji:"), e);
        grid.addRow(6, new Label("URL MP3:"), u);

        // Pasang rancangan Grid tadi sebagai konten utama dari Dialog Pane
        dialog.getDialogPane().setContent(grid);

        // Result Converter: Mengonversi/mengubah inputan string teks dari form menjadi Objek 'Song' utuh
        // fungsi ini dijalankan tepat saat tombol "Save to Supabase" diklik oleh user
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return new Song(t.getText(), a.getText(), al.getText(), d.getText(), g.getText(), e.getText());
            }
            return null; // Return null jika user menekan tombol CANCEL
        });

        // Menampilkan Dialog ke layar (Blocking Mode) dan menunggu respon aksi dari pengguna
        dialog.showAndWait().ifPresent(s -> {
            // Blok ini tereksekusi hanya jika objek song berhasil dibuat (bukan null)
            s.setFileUrl(u.getText().trim()); // Ambil url lagu, bersihkan spasi kosong di ujung teks
            controller.databaseManager.saveSongToSupabase(s); // Oper objek lagu ke DatabaseManager untuk disave
        });
    }
}