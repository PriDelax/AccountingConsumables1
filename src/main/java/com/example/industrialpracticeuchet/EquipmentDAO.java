package com.example.industrialpracticeuchet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDAO {
    public static class Equipment {
        public int id;
        public String name;
        public String categoryName;
        public String article;
        public int minStock;
        public int currentStock;
        public String image_path;
        @Override
        public String toString() {
            return name + " (" + article + ")";
        }
    }
    public static class PurchaseItem {
        public String name;
        public String article;
        public int currentStock;
        public int minStock;
        public int recommendedQty;
    }

    public void create(String name, String categoryName, String article, int minStock, int initialStock) throws SQLException {
        CategoryDAO categoryDAO = new CategoryDAO();
        Integer categoryId = categoryDAO.findIdByName(categoryName);
        if (categoryId == null) {
            categoryDAO.create(categoryName);
            categoryId = categoryDAO.findIdByName(categoryName);
        }
        String sql = """
            INSERT INTO equipment (name, category_id, article, min_stock, current_stock)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, categoryId);
            stmt.setString(3, article);
            stmt.setInt(4, minStock);
            stmt.setInt(5, initialStock);
            stmt.executeUpdate();
        }
    }

    public List<Equipment> findAll() throws SQLException {
        String sql = """
            SELECT e.id, e.name, e.article, e.min_stock, e.current_stock, c.name AS category_name, e.image_path
            FROM equipment e
            LEFT JOIN categories c ON e.category_id = c.id
            ORDER BY e.name
            """;
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Equipment eq = new Equipment();
                eq.id = rs.getInt("id");
                eq.name = rs.getString("name");
                eq.article = rs.getString("article");
                eq.minStock = rs.getInt("min_stock");
                eq.currentStock = rs.getInt("current_stock");
                eq.categoryName = rs.getString("category_name");
                eq.image_path = rs.getString("image_path");
                list.add(eq);
            }
        }
        return list;
    }

    public List<PurchaseItem> getItemsForPurchase() throws SQLException {
        String sql = """
        SELECT name, article, current_stock, min_stock
        FROM equipment
        WHERE current_stock <= min_stock AND min_stock > 0
        ORDER BY name
        """;
        List<PurchaseItem> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PurchaseItem item = new PurchaseItem();
                item.name = rs.getString("name");
                item.article = rs.getString("article");
                item.currentStock = rs.getInt("current_stock");
                item.minStock = rs.getInt("min_stock");
                item.recommendedQty = Math.max(1, item.minStock * 2 - item.currentStock);
                list.add(item);
            }
        }
        return list;
    }

    public boolean delete(int id) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM stock_operations WHERE equipment_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return false;
                }
            }
        }
        String deleteSql = "DELETE FROM equipment WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
            delStmt.setInt(1, id);
            return delStmt.executeUpdate() > 0;
        }
    }

    public List<Equipment> search(String query) throws SQLException {
        String sql = """
        SELECT e.id, e.name, e.article, e.min_stock, e.current_stock, c.name AS category_name, e.image_path
        FROM equipment e
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.name LIKE ? OR e.article LIKE ?
        ORDER BY e.name
        """;
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Equipment eq = new Equipment();
                    eq.id = rs.getInt("id");
                    eq.name = rs.getString("name");
                    eq.article = rs.getString("article");
                    eq.minStock = rs.getInt("min_stock");
                    eq.currentStock = rs.getInt("current_stock");
                    eq.categoryName = rs.getString("category_name");
                    eq.image_path = rs.getString("image_path");
                    list.add(eq);
                }
            }
        }
        return list;
    }

    public Equipment findById(int id) throws SQLException {
        String sql = """
            SELECT e.id, e.name, e.article, e.min_stock, e.current_stock, c.name AS category_name, e.image_path
            FROM equipment e
            LEFT JOIN categories c ON e.category_id = c.id
            WHERE e.id = ?
            """;
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Equipment eq = new Equipment();
                    eq.id = rs.getInt("id");
                    eq.name = rs.getString("name");
                    eq.article = rs.getString("article");
                    eq.minStock = rs.getInt("min_stock");
                    eq.currentStock = rs.getInt("current_stock");
                    eq.categoryName = rs.getString("category_name");
                    eq.image_path = rs.getString("image_path");
                    return eq;
                }
            }
        }
        return null;
    }

    public void updateStock(int equipmentId, int newStock) throws SQLException {
        String sql = "UPDATE equipment SET current_stock = ? WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newStock);
            stmt.setInt(2, equipmentId);
            stmt.executeUpdate();
        }
    }

    public boolean hasEnoughStock(int equipmentId, int quantity) throws SQLException {
        Equipment eq = findById(equipmentId);
        return eq != null && eq.currentStock >= quantity;
    }
}