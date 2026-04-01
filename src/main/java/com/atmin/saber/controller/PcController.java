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

            printPcListAsTable(pcs);

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

            printPcListAsTable(pcs);
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
            PCStatus status = input.status() != null ? input.status() : PCStatus.AVAILABLE;
            pcService.add(new PC(0, input.pcName(), null, input.roomName(), status, input.configuration()));
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

            boolean hasUpdate = !isBlank(input.newName())
                    || !isBlank(input.newRoomName())
                    || input.newStatus() != null
                    || !isBlank(input.newConfiguration());

            if (!hasUpdate) {
                System.out.println("\tNo fields were updated. Keeping old data.");
                return;
            }

            PC updated = new PC();
            updated.setPcId(oldPc.getPcId());
            updated.setPcName(isBlank(input.newName()) ? oldPc.getPcName() : input.newName().trim());
            updated.setRoomName(isBlank(input.newRoomName()) ? oldPc.getRoomName() : input.newRoomName().trim());
            updated.setStatus(input.newStatus() == null ? oldPc.getStatus() : input.newStatus());
            updated.setConfiguration(isBlank(input.newConfiguration()) ? oldPc.getConfiguration() : input.newConfiguration().trim());

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

    private static void printPcTableHeader() {
        System.out.println("\n+------+----------------------+----------------------+--------------+--------------------------------------------------+");
        System.out.printf("| %-4s | %-20s | %-20s | %-12s | %-48s |%n", "ID", "PC NAME", "ROOM NAME", "STATUS", "CONFIGURATION");
        System.out.println("+------+----------------------+----------------------+--------------+--------------------------------------------------+");
    }

    private static void printPcTableFooter() {
        System.out.println("+------+----------------------+----------------------+--------------+--------------------------------------------------+");
    }

    private static void printPcRow(PC pc) {
        System.out.printf("| %-4d | %-20s | %-20s | %-12s | %-48s |%n",
                pc.getPcId(),
                safeLength(pc.getPcName(), 20),
                safeLength(pc.getRoomName(), 20),
                pc.getStatus() == null ? "" : pc.getStatus().name(),
                safeLength(pc.getConfiguration(), 48));
    }

    public static void printPcAsTable(PC pc) {
        printPcTableHeader();
        printPcRow(pc);
        printPcTableFooter();
    }

    public static void printPcListAsTable(List<PC> pcs) {
        if (pcs == null || pcs.isEmpty()) return;
        printPcTableHeader();
        for (PC pc : pcs) {
            printPcRow(pc);
        }
        printPcTableFooter();
    }

    public static String safeLength(String s, int maxLen) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.length() <= maxLen) return trimmed;
        return trimmed.substring(0, maxLen - 3) + "...";
    }

    public static String safeShort(String s) {
        return safeLength(s, 20);
    }
}

