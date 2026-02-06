package com.example.industrialpracticeuchet;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:storage.db";
    static {
        try {
            init();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void init() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            String createCategoriesTable = """
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
                """;
            String createEquipmentTable = """
                CREATE TABLE IF NOT EXISTS equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category_id INTEGER,
                    article TEXT,
                    min_stock INTEGER DEFAULT 2,
                    current_stock INTEGER DEFAULT 0,
                    image_path TEXT,
                    FOREIGN KEY(category_id) REFERENCES categories(id)
                );
                """;
            String createRequestsTable = """
                CREATE TABLE IF NOT EXISTS requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK(quantity > 0),
                    requester TEXT NOT NULL,
                    reason TEXT,
                    status TEXT CHECK(status IN ('ОЖИДАЕТСЯ', 'ВЫПОЛНЕНО', 'ОТКЛОНЕНО')) DEFAULT 'ОЖИДАЕТСЯ',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    fulfilled_at DATETIME,
                    fulfilled_by TEXT,
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id)
                );
                """;
            String createStockOperationsTable = """
                CREATE TABLE IF NOT EXISTS stock_operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id INTEGER NOT NULL,
                    operation_type TEXT CHECK(operation_type IN ('ПОСТУПЛЕНИЕ', 'СПИСАНИЕ')),
                    quantity INTEGER NOT NULL,
                    balance_after INTEGER,
                    destination TEXT,                -- куда списано (при OUT)
                    reason TEXT,                     -- причина списания
                    document_ref TEXT,               -- накладная и т.п.
                    performed_by TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    request_id INTEGER,              -- ссылка на заявку (если списание по заявке)
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(request_id) REFERENCES requests(id)
                );
                """;
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    role TEXT CHECK(role IN ('ПОЛЬЗОВАТЕЛЬ', 'АДМИН')) DEFAULT 'ПОЛЬЗОВАТЕЛЬ'
                );
                """;
            stmt.execute(createCategoriesTable);
            stmt.execute(createEquipmentTable);
            stmt.execute(createRequestsTable);
            stmt.execute(createStockOperationsTable);
            stmt.execute(createUsersTable);
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'АДМИН'")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO users (name, role) VALUES ('Администратор', 'АДМИН')");
                    System.out.println("Создан пользователь по умолчанию: Администратор");
                }
            }
            System.out.println("База данных инициализирована");
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}

