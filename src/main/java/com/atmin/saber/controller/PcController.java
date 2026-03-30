package com.atmin.saber.controller;

import com.atmin.saber.dao.impl.PcDaoImpl;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.presentation.PcManagementMenu;
import com.atmin.saber.service.PcService;
import com.atmin.saber.service.impl.PcServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class PcController {
    private final PcService pcService;

    public PcController(PcService pcService) {
        this.pcService = pcService;
    }

    public static PcController createDefault() {
        return new PcController(new PcServiceImpl(new PcDaoImpl(DBConnection.getInstance())));
    }
    public PcService getPcService() {
        return pcService;
    }

    public void listPcs() {
        try {
            List<PC> pcs = pcService.getAllActive();
            if (pcs.isEmpty()) {
                System.out.println("\tThe PC list is currently empty.");
                return;
            }

            System.out.println("\n+------+----------------------+----------------------+--------------+");
            System.out.printf("| %-4s | %-20s | %-20s | %-12s |%n", "ID", "PC NAME", "ROOM NAME", "STATUS");
            System.out.println("+------+----------------------+----------------------+--------------+");

            for (PC pc : pcs) {
                System.out.printf("| %-4d | %-20s | %-20s | %-12s |%n",
                        pc.getPcId(),
                        safeShort(pc.getPcName()),
                        safeShort(pc.getRoomName()),
                        pc.getStatus() == null ? "" : pc.getStatus().name());
            }
            System.out.println("+------+----------------------+----------------------+--------------+");

        } catch (RuntimeException ex) {
            System.out.println("\tFailed to load PC list: " + ex.getMessage());
        }
    }

    public void listPcsByRoomId(int roomId) {
        try {
            List<PC> pcs = pcService.getAllActiveByRoomId(roomId);
            if (pcs.isEmpty()) {
                System.out.println("\tNo PCs found for this room.");
                return;
            }

            System.out.println("\n+------+----------------------+----------------------+--------------+----------------------+--------+----------------------+" );
            System.out.printf("| %-4s | %-20s | %-20s | %-12s | %-20s | %-6s | %-20s |%n",
                    "ID", "PC NAME", "ROOM NAME", "STATUS", "CPU", "RAM", "GPU");
            System.out.println("+------+----------------------+----------------------+--------------+----------------------+--------+----------------------+" );
            for (PC pc : pcs) {
                String cpu = pc.getSpec() == null ? "" : safeShort(pc.getSpec().getCpu());
                String ram = pc.getSpec() == null || pc.getSpec().getRamGb() == null ? "" : (pc.getSpec().getRamGb() + "GB");
                String gpu = pc.getSpec() == null ? "" : safeShort(pc.getSpec().getGpu());
                System.out.printf("| %-4d | %-20s | %-20s | %-12s | %-20s | %-6s | %-20s |%n",
                        pc.getPcId(),
                        safeShort(pc.getPcName()),
                        safeShort(pc.getRoomName()),
                        pc.getStatus() == null ? "" : pc.getStatus().name(),
                        cpu,
                        ram,
                        gpu);
            }
            System.out.println("+------+----------------------+----------------------+--------------+----------------------+--------+----------------------+" );
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to load PCs: " + ex.getMessage());
        }
    }

    public void addPc(PcManagementMenu.AddPcInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for adding a PC.");
                return;
            }
            pcService.add(new PC(0, input.pcName(), input.roomName(), input.status()));
            System.out.println("\tPC added successfully!");
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to add PC: " + ex.getMessage());
        }
    }

    public void editPc(PcManagementMenu.EditPcInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for editing a PC.");
                return;
            }
            PC oldPc = pcService.getById(input.pcId()).orElse(null);
            if (oldPc == null || oldPc.getStatus() == PCStatus.DELETED) {
                return;
            }

            PC updated = new PC();
            updated.setPcId(oldPc.getPcId());
            updated.setPcName(isBlank(input.newName()) ? oldPc.getPcName() : input.newName().trim());
            updated.setRoomName(isBlank(input.newRoomName()) ? oldPc.getRoomName() : input.newRoomName().trim());
            updated.setStatus(input.newStatus() == null ? oldPc.getStatus() : input.newStatus());

            pcService.update(updated);
            System.out.println("\tUpdated successfully!");
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to update PC: " + ex.getMessage());
        }
    }

    public void deletePc(PcManagementMenu.DeletePcInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for deleting a PC.");
                return;
            }
            PC pc = pcService.getById(input.pcId()).orElse(null);
            if (pc == null || pc.getStatus() == PCStatus.DELETED) {
                return;
            }

            if (!isYes(input.confirm())) {
                System.out.println("\tDelete canceled.");
                return;
            }

            pcService.delete(input.pcId());
            System.out.println("\tPC deleted successfully!");
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to delete PC: " + ex.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isYes(String s) {
        return s != null && s.trim().equalsIgnoreCase("Y");
    }

    public static void printPcAsTable(PC pc) {
        System.out.println("\n+------+----------------------+----------------------+--------------+");
        System.out.printf("| %-4s | %-20s | %-20s | %-12s |%n", "ID", "PC NAME", "ROOM NAME", "STATUS");
        System.out.println("+------+----------------------+----------------------+--------------+");
        System.out.printf("| %-4d | %-20s | %-20s | %-12s |%n",
                pc.getPcId(),
                safeShort(pc.getPcName()),
                safeShort(pc.getRoomName()),
                pc.getStatus() == null ? "" : pc.getStatus().name());
        System.out.println("+------+----------------------+----------------------+--------------+");
    }

    public static String safeShort(String s) {
        if (s == null) return "";
        if (s.length() <= 20) return s;
        return s.substring(0, 17) + "...";
    }
}

