package com.example.industrialpracticeuchet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public static class User {
        public int id;
        public String name;
        public String role;

        public boolean isAdmin() {
            return "АДМИН".equals(role);
        }
        @Override
        public String toString() {
            return name + (isAdmin() ? " (Админ)" : "");
        }
    }

    public void createUser(String name, boolean isAdmin) throws SQLException {
        String role = isAdmin ? "АДМИН" : "ПОЛЬЗОВАТЕЛЬ";
        String sql = "INSERT INTO users (name, role) VALUES (?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            stmt.setString(2, role);
            stmt.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, role FROM users ORDER BY name";
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User u = new User();
                u.id = rs.getInt("id");
                u.name = rs.getString("name");
                u.role = rs.getString("role");
                users.add(u);
            }
        }
        return users;
    }

    public User findByName(String name) throws SQLException {
        String sql = "SELECT id, name, role FROM users WHERE name = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.id = rs.getInt("id");
                    u.name = rs.getString("name");
                    u.role = rs.getString("role");
                    return u;
                }
            }
        }
        return null;
    }

    public void updateName(String oldName, String newName) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName.trim());
            stmt.setString(2, oldName);
            stmt.executeUpdate();
        }
    }
}