package com.atmin.saber.model;

import com.atmin.saber.model.enums.PCStatus;

public class PC {
    private int pcId;
    private String pcName;
    private Integer roomId;
    private String roomName;
    private PCStatus status;
    private String configuration;

    public PC() {
    }

    public PC(int pcId, String pcName, Integer roomId, String roomName, PCStatus status, String configuration) {
        this.pcId = pcId;
        this.pcName = pcName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.status = status;
        this.configuration = configuration;
    }

    public int getPcId() {
        return pcId;
    }

    public void setPcId(int pcId) {
        this.pcId = pcId;
    }

    public String getPcName() {
        return pcName;
    }

    public void setPcName(String pcName) {
        this.pcName = pcName;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public PCStatus getStatus() {
        return status;
    }

    public void setStatus(PCStatus status) {
        this.status = status;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return "PC{" +
                "pcId=" + pcId +
                ", pcName='" + pcName + '\'' +
                ", roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                ", status=" + status +
                ", configuration='" + configuration + '\'' +
                '}';
    }
}
