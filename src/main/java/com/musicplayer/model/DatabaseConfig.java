package com.musicplayer.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    
    // URL dan USER aman ditaruh sini karena bukan rahasia fatal
    private static final String URL = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres";
    private static final String USER = "postgres.qtqtumxfjrsctvggbktu";

    public static Connection getConnection() throws SQLException {
        // 1. Panggil alat pembaca file
        Properties props = new Properties();
        
        // 2. Baca password dari file rahasia
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("File config.properties ga ketemu bre! Bikin dulu di folder luar.", e);
        }

        // 3. Ambil nilai passwordnya
        String password = props.getProperty("DB_PASSWORD");

        // 4. Konek ke Supabase
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL Driver ga ketemu bre!", e);
        }
    }
}