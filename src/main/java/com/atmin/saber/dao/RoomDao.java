package com.atmin.saber.dao;

import com.atmin.saber.model.Room;

import java.util.List;

public interface RoomDao {
    /**
     * Load all rooms for rendering selection menus.
     */
    List<Room> findAll();
}

