package com.atmin.saber.model;

import com.atmin.saber.model.enums.PCStatus;

public class PC {
    /**
     * PC hardware configuration/spec (optional). Backed by DB table pc_specs.
     * Nested here to avoid adding more files.
     */
    public static class Spec {
        private int pcId;
        private String cpu;
        private Integer ramGb;
        private String gpu;
        private Integer storageGb;
        private Integer monitorHz;
        private String os;
        private String notes;

        public Spec() {
        }

        public Spec(int pcId, String cpu, Integer ramGb, String gpu, Integer storageGb, Integer monitorHz, String os, String notes) {
            this.pcId = pcId;
            this.cpu = cpu;
            this.ramGb = ramGb;
            this.gpu = gpu;
            this.storageGb = storageGb;
            this.monitorHz = monitorHz;
            this.os = os;
            this.notes = notes;
        }

        public int getPcId() {
            return pcId;
        }

        public void setPcId(int pcId) {
            this.pcId = pcId;
        }

        public String getCpu() {
            return cpu;
        }

        public void setCpu(String cpu) {
            this.cpu = cpu;
        }

        public Integer getRamGb() {
            return ramGb;
        }

        public void setRamGb(Integer ramGb) {
            this.ramGb = ramGb;
        }

        public String getGpu() {
            return gpu;
        }

        public void setGpu(String gpu) {
            this.gpu = gpu;
        }

        public Integer getStorageGb() {
            return storageGb;
        }
        public void setStorageGb(Integer storageGb) {
            this.storageGb = storageGb;
        }

        public Integer getMonitorHz() {
            return monitorHz;
        }

        public void setMonitorHz(Integer monitorHz) {
            this.monitorHz = monitorHz;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    private int pcId;
    private String pcName;
    private Integer roomId;
    private String roomName;
    private PCStatus status;
    private Spec spec;

    public PC() {
    }

    public PC(int pcId, String pcName, String roomName, PCStatus status) {
        this.pcId = pcId;
        this.pcName = pcName;
        this.roomName = roomName;
        this.status = status;
    }

    public PC(int pcId, String pcName, Integer roomId, String roomName, PCStatus status, Spec spec) {
        this.pcId = pcId;
        this.pcName = pcName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.status = status;
        this.spec = spec;
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

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    public PCStatus getStatus() {
        return status;
    }

    public void setStatus(PCStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PC{" +
                "pcId=" + pcId +
                ", pcName='" + pcName + '\'' +
                ", roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                ", status=" + status +
                ", spec=" + (spec == null ? "null" : "spec") +
                '}';
    }
}

