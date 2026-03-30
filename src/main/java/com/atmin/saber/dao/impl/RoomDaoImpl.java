package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.RoomDao;
import com.atmin.saber.model.Room;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDaoImpl implements RoomDao {

    private final DBConnection db;

    public RoomDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT room_id, room_name FROM rooms ORDER BY room_id";
        List<Room> rooms = new ArrayList<>();

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rooms.add(new Room(rs.getInt("room_id"), rs.getString("room_name")));
            }
            return rooms;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load rooms: " + e.getMessage(), e);
        }
    }
}

