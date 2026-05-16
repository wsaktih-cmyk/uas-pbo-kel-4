package com.musicplayer.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // Ganti URL di bawah ini pakai JDBC Connection String dari Supabase lu!
    private static final String URL = "postgresql://postgres.qtqtumxfjrsctvggbktu:[YOUR-PASSWORD]@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "tugaspbo1922";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL tidak ditemukan!", e);
        }
    }
}