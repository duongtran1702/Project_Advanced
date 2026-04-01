package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.RoomDao;
import com.atmin.saber.dao.base.BaseDao;
import com.atmin.saber.dao.base.SqlConstants;
import com.atmin.saber.model.Room;
import com.atmin.saber.util.DBConnection;

import java.util.ArrayList;
import java.util.List;

public class RoomDaoImpl extends BaseDao implements RoomDao {

    public RoomDaoImpl(DBConnection db) {
        super(db);
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT " + SqlConstants.ROOM_COLUMNS + " FROM rooms ORDER BY room_id";
        return executeQuery(sql, rs -> {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(new Room(rs.getInt("room_id"), rs.getString("room_name")));
            }
            return rooms;
        });
    }
}

