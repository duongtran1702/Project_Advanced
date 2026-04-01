package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.PcDao;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PcDaoImpl implements PcDao {

    private final DBConnection db;

    public PcDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public List<PC> findAllActive() {
        String sql = "SELECT p.pc_id, p.pc_name, p.zone_id AS room_id, r.room_name AS room_name, p.status " +
                "FROM pcs p JOIN rooms r ON p.zone_id = r.room_id " +
                "WHERE p.status <> 'DELETED' ORDER BY p.pc_id DESC";
        List<PC> pcs = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pcs.add(mapPc(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pcs: " + e.getMessage(), e);
        }
        return pcs;
    }

    @Override
    public List<PC> findAllActiveByRoomId(int roomId) {
        String sql = "SELECT p.pc_id, p.pc_name, p.zone_id AS room_id, r.room_name AS room_name, p.status " +
                "FROM pcs p JOIN rooms r ON p.zone_id = r.room_id " +
                "WHERE p.status <> 'DELETED' AND p.zone_id = ? ORDER BY p.pc_id DESC";
        List<PC> pcs = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pcs.add(mapPc(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pcs: " + e.getMessage(), e);
        }
        return pcs;
    }

    @Override
    public Optional<PC> findById(int pcId) {
        String sql = "SELECT p.pc_id, p.pc_name, p.zone_id AS room_id, r.room_name AS room_name, p.status " +
                "FROM pcs p JOIN rooms r ON p.zone_id = r.room_id " +
                "WHERE p.pc_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPc(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find pc: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(int pcId) {
        String sql = "SELECT 1 FROM pcs WHERE pc_id = ? LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check pc existence: " + e.getMessage(), e);
        }
    }

    @Override
    public void insert(PC pc) {
        // pcs.pc_id is AUTO_INCREMENT (INT) => do NOT insert pc_id manually
        // Store zone_id in DB but expose zone_name to the app
        String sql = "INSERT INTO pcs(pc_name, zone_id, status) " +
                "VALUES(?, (SELECT room_id FROM rooms WHERE room_name = ? LIMIT 1), ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pc.getPcName());
            ps.setString(2, pc.getRoomName());
            ps.setString(3, pc.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert pc: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(PC pc) {
        // Update zone_id based on provided zone_name
        String sql = "UPDATE pcs SET pc_name = ?, zone_id = (SELECT room_id FROM rooms WHERE room_name = ? LIMIT 1), status = ? WHERE pc_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pc.getPcName());
            ps.setString(2, pc.getRoomName());
            ps.setString(3, pc.getStatus().name());
            ps.setInt(4, pc.getPcId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update pc: " + e.getMessage(), e);
        }
    }

    @Override
    public void softDelete(int pcId) {
        String sql = "UPDATE pcs SET status = 'DELETED' WHERE pc_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete pc: " + e.getMessage(), e);
        }
    }

    private PC mapPc(ResultSet rs) throws SQLException {
        PC pc = new PC();
        pc.setPcId(rs.getInt("pc_id"));
        pc.setPcName(rs.getString("pc_name"));
        try {
            pc.setRoomId(rs.getInt("room_id"));
        } catch (SQLException ignored) {
            // backward compatibility if column alias isn't present
        }
        pc.setRoomName(rs.getString("room_name"));

        String statusStr = rs.getString("status");
        try {
            pc.setStatus(PCStatus.valueOf(statusStr));
        } catch (IllegalArgumentException ex) {
            // Defensive: unknown status in DB. Do not crash the app.
            pc.setStatus(PCStatus.MAINTENANCE);
        }
        return pc;
    }
}


