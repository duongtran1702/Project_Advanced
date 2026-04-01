package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.PcDao;
import com.atmin.saber.dao.base.BaseDao;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.util.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PcDaoImpl extends BaseDao implements PcDao {

    private static final String PC_QUERY =
        "SELECT p.pc_id, p.pc_name, p.zone_id AS room_id, r.room_name AS room_name, p.status, p.configuration " +
        "FROM pcs p JOIN rooms r ON p.zone_id = r.room_id " +
        "WHERE p.status <> 'DELETED'";

    public PcDaoImpl(DBConnection db) {
        super(db);
    }

    @Override
    public List<PC> findAllActive() {
        String sql = PC_QUERY + " ORDER BY p.pc_id DESC";
        return executeQuery(sql, rs -> {
            List<PC> pcs = new ArrayList<>();
            while (rs.next()) {
                pcs.add(mapPc(rs));
            }
            return pcs;
        });
    }

    @Override
    public List<PC> findAllActiveByRoomId(int roomId) {
        String sql = PC_QUERY + " AND p.zone_id = ? ORDER BY p.pc_id DESC";
        return executeQuery(sql, rs -> {
            List<PC> pcs = new ArrayList<>();
            while (rs.next()) {
                pcs.add(mapPc(rs));
            }
            return pcs;
        }, roomId);
    }

    @Override
    public Optional<PC> findById(int pcId) {
        String sql = PC_QUERY + " AND p.pc_id = ?";
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return Optional.of(mapPc(rs));
            }
            return Optional.empty();
        }, pcId);
    }

    @Override
    public void insert(PC pc) {
        // pcs.pc_id is AUTO_INCREMENT (INT) => do NOT insert pc_id manually
        // Store zone_id in DB but expose zone_name to the app
        String sql = "INSERT INTO pcs(pc_name, zone_id, status, configuration) " +
                "VALUES(?, (SELECT room_id FROM rooms WHERE room_name = ? LIMIT 1), ?, ?)";
        executeUpdate(sql, pc.getPcName(), pc.getRoomName(), pc.getStatus().name(), pc.getConfiguration());
    }

    @Override
    public void update(PC pc) {
        // Update zone_id based on provided zone_name
        String sql = "UPDATE pcs SET pc_name = ?, zone_id = (SELECT room_id FROM rooms WHERE room_name = ? LIMIT 1), status = ?, configuration = ? WHERE pc_id = ?";
        executeUpdate(sql, pc.getPcName(), pc.getRoomName(), pc.getStatus().name(), pc.getConfiguration(), pc.getPcId());
    }

    @Override
    public void softDelete(int pcId) {
        String sql = "UPDATE pcs SET status = 'DELETED' WHERE pc_id = ?";
        executeUpdate(sql, pcId);
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
        pc.setConfiguration(rs.getString("configuration"));
        return pc;
    }
}


