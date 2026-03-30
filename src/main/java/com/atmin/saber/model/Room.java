package com.atmin.saber.model;

/**
 * Room entity (read-only for now): represents a PC room/area.
 * DB table: rooms(room_id, room_name, base_price, type)
 */
public class Room {
    private int roomId;
    private String roomName;

    public Room() {
    }

    public Room(int roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                '}';
    }
}

