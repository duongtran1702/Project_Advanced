package com.atmin.saber.model;

/**
 * Room entity (read-only for now): represents a PC room/area.
 * DB table: rooms(room_id, room_name, base_price, type)
 */
public record Room(int roomId, String roomName) {

}

