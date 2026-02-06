package com.example.industrialpracticeuchet;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {
    public enum Status {
        ОЖИДАЕТСЯ, ВЫПОЛНЕНО, ОТКЛОНЕНО
    }
    public static class Request {
        public int id;
        public int equipmentId;
        public String equipmentName;
        public int quantity;
        public String requester;
        public String reason;
        public Status status;
        public LocalDateTime createdAt;
        public LocalDateTime fulfilledAt;
        public String fulfilledBy;
    }

    public void create(int equipmentId, int quantity, String requester, String reason) throws SQLException {
        String sql = """
            INSERT INTO requests (equipment_id, quantity, requester, reason)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, equipmentId);
            stmt.setInt(2, quantity);
            stmt.setString(3, requester);
            stmt.setString(4, reason);
            stmt.executeUpdate();
        }
    }

    public List<Request> findByStatus(Status status) throws SQLException {
        String sql = """
            SELECT r.id, r.equipment_id, e.name AS equipment_name, r.quantity, r.requester,
                   r.reason, r.status, r.created_at, r.fulfilled_at, r.fulfilled_by
            FROM requests r
            JOIN equipment e ON r.equipment_id = e.id
            WHERE r.status = ?
            ORDER BY r.created_at DESC
            """;
        List<Request> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Request req = new Request();
                    req.id = rs.getInt("id");
                    req.equipmentId = rs.getInt("equipment_id");
                    req.equipmentName = rs.getString("equipment_name");
                    req.quantity = rs.getInt("quantity");
                    req.requester = rs.getString("requester");
                    req.reason = rs.getString("reason");
                    req.status = Status.valueOf(rs.getString("status"));
                    req.createdAt = parseDateTime(rs.getString("created_at"));
                    req.fulfilledAt = parseDateTime(rs.getString("fulfilled_at"));
                    req.fulfilledBy = rs.getString("fulfilled_by");
                    list.add(req);
                }
            }
        }
        return list;
    }

    public void fulfill(int requestId, String fulfilledBy) throws SQLException {
        EquipmentDAO equipmentDAO = new EquipmentDAO();
        Request request = findById(requestId);
        if (request == null || request.status != Status.ОЖИДАЕТСЯ) {
            throw new SQLException("Заявка не найдена или уже обработана");
        }
        if (!equipmentDAO.hasEnoughStock(request.equipmentId, request.quantity)) {
            throw new SQLException("Недостаточно остатка на складе");
        }
        EquipmentDAO.Equipment eq = equipmentDAO.findById(request.equipmentId);
        equipmentDAO.updateStock(request.equipmentId, eq.currentStock - request.quantity);
        StockOperationDAO stockOpDAO = new StockOperationDAO();
        stockOpDAO.createOut(
                request.equipmentId,
                request.quantity,
                request.requester,
                "Выдача по заявке #" + requestId,
                fulfilledBy,
                requestId
        );
        String sql = """
            UPDATE requests
            SET status = 'ВЫПОЛНЕНО', fulfilled_at = CURRENT_TIMESTAMP, fulfilled_by = ?
            WHERE id = ?
            """;
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fulfilledBy);
            stmt.setInt(2, requestId);
            stmt.executeUpdate();
        }
    }

    public Request findById(int id) throws SQLException {
        String sql = """
            SELECT r.id, r.equipment_id, e.name AS equipment_name, r.quantity, r.requester,
                   r.reason, r.status, r.created_at, r.fulfilled_at, r.fulfilled_by
            FROM requests r
            JOIN equipment e ON r.equipment_id = e.id
            WHERE r.id = ?
            """;
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Request req = new Request();
                    req.id = rs.getInt("id");
                    req.equipmentId = rs.getInt("equipment_id");
                    req.equipmentName = rs.getString("equipment_name");
                    req.quantity = rs.getInt("quantity");
                    req.requester = rs.getString("requester");
                    req.reason = rs.getString("reason");
                    req.status = Status.valueOf(rs.getString("status"));
                    req.createdAt = parseDateTime(rs.getString("created_at"));
                    req.fulfilledAt = parseDateTime(rs.getString("fulfilled_at"));
                    req.fulfilledBy = rs.getString("fulfilled_by");
                    return req;
                }
            }
        }
        return null;
    }

    public void cancel(int requestId, String requester) throws SQLException {
        String sql = """
        UPDATE requests
        SET status = 'ОТКЛОНЕНО', fulfilled_at = CURRENT_TIMESTAMP, fulfilled_by = ?
        WHERE id = ? AND requester = ? AND status = 'ОЖИДАЕТСЯ'
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requester);
            stmt.setInt(2, requestId);
            stmt.setString(3, requester);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Заявка не найдена или уже обработана");
            }
        }
    }

    private LocalDateTime parseDateTime(String dtStr) {
        if (dtStr == null || dtStr.isEmpty()) return null;
        return LocalDateTime.parse(dtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
