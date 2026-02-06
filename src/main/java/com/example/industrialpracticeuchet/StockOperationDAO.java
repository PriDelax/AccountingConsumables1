package com.example.industrialpracticeuchet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockOperationDAO {
    public static class Operation {
        public int id;
        public String materialName;
        public String operationType;
        public int quantity;
        public int balanceAfter;
        public String destination;
        public String reason;
        public String performedBy;
        public String createdAt;
    }

    public void createIn(int equipmentId, int quantity, String documentRef, String performedBy) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть > 0");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int currentStock = getCurrentStock(conn, equipmentId);
                int newStock = currentStock + quantity;
                updateEquipmentStock(conn, equipmentId, newStock);
                String sql = """
                    INSERT INTO stock_operations (
                        equipment_id, operation_type, quantity, balance_after,
                        destination, reason, document_ref, performed_by, request_id
                    ) VALUES (?, 'ПОСТУПЛЕНИЕ', ?, ?, NULL, 'Поступление', ?, ?, NULL)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, equipmentId);
                    stmt.setInt(2, quantity);
                    stmt.setInt(3, newStock);
                    stmt.setString(4, documentRef);
                    stmt.setString(5, performedBy);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Ошибка поступления: " + e.getMessage(), e);
            }
        }
    }


    public void createOut(int equipmentId, int quantity, String destination, String reason, String performedBy, Integer requestId) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть > 0");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int currentStock = getCurrentStock(conn, equipmentId);
                if (currentStock < quantity) {
                    throw new SQLException("Недостаточно остатка на складе. Доступно: " + currentStock);
                }
                int newStock = currentStock - quantity;
                updateEquipmentStock(conn, equipmentId, newStock);
                String sql = """
                    INSERT INTO stock_operations (
                        equipment_id, operation_type, quantity, balance_after,
                        destination, reason, document_ref, performed_by, request_id
                    ) VALUES (?, 'СПИСАНИЕ', ?, ?, ?, ?, NULL, ?, ?)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, equipmentId);
                    stmt.setInt(2, quantity);
                    stmt.setInt(3, newStock);
                    stmt.setString(4, destination);
                    stmt.setString(5, reason);
                    stmt.setString(6, performedBy);
                    stmt.setObject(7, requestId, Types.INTEGER);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Ошибка списания: " + e.getMessage(), e);
            }
        }
    }

    private int getCurrentStock(Connection conn, int equipmentId) throws SQLException {
        String sql = "SELECT current_stock FROM equipment WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, equipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("current_stock");
                } else {
                    throw new SQLException("Материал не найден");
                }
            }
        }
    }

    private void updateEquipmentStock(Connection conn, int equipmentId, int newStock) throws SQLException {
        String sql = "UPDATE equipment SET current_stock = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newStock);
            stmt.setInt(2, equipmentId);
            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Не удалось обновить остаток");
            }
        }
    }


    public List<Operation> findAll(String operationType) throws SQLException {
        StringBuilder sql = new StringBuilder("""
        SELECT 
            so.id, e.name AS material_name, so.operation_type, so.quantity,
            so.balance_after, so.destination, so.reason, so.performed_by, so.created_at
        FROM stock_operations so
        JOIN equipment e ON so.equipment_id = e.id
        """);
        if (operationType != null) {
            sql.append(" WHERE so.operation_type = ?");
        }
        sql.append(" ORDER BY so.created_at DESC LIMIT 500");
        List<Operation> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (operationType != null) {
                stmt.setString(1, operationType);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Operation op = new Operation();
                    op.id = rs.getInt("id");
                    op.materialName = rs.getString("material_name");
                    op.operationType = rs.getString("operation_type");
                    op.quantity = rs.getInt("quantity");
                    op.balanceAfter = rs.getInt("balance_after");
                    op.destination = rs.getString("destination");
                    op.reason = rs.getString("reason");
                    op.performedBy = rs.getString("performed_by");
                    op.createdAt = rs.getString("created_at");
                    list.add(op);
                }
            }
        }
        return list;
    }
    // Перегрузка без фильтра
    public List<Operation> findAll() throws SQLException {
        return findAll(null);
    }

    public List<Operation> findByUser(String userName) throws SQLException {
        String sql = """
        SELECT s.id, e.name AS material_name, s.operation_type, s.quantity, s.balance_after,
               s.destination, s.reason, s.performed_by, s.created_at
        FROM stock_operations s
        JOIN equipment e ON s.equipment_id = e.id
        WHERE s.performed_by = ? OR s.destination = ?
        ORDER BY s.created_at DESC
        """;
        List<Operation> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userName);
            stmt.setString(2, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Operation op = new Operation();
                    op.id = rs.getInt("id");
                    op.materialName = rs.getString("material_name");
                    op.operationType = rs.getString("operation_type");
                    op.quantity = rs.getInt("quantity");
                    op.balanceAfter = rs.getInt("balance_after");
                    op.destination = rs.getString("destination");
                    op.reason = rs.getString("reason");
                    op.performedBy = rs.getString("performed_by");
                    op.createdAt = rs.getString("created_at");
                    list.add(op);
                }
            }
        }
        return list;
    }
}
