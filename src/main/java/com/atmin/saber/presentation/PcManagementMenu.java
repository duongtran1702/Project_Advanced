package com.atmin.saber.presentation;

import com.atmin.saber.controller.PcController;
import com.atmin.saber.dao.RoomDao;
import com.atmin.saber.dao.impl.RoomDaoImpl;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.Room;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.util.ConsoleInput;
import com.atmin.saber.util.DBConnection;

import java.util.List;
import java.util.Scanner;

import static com.atmin.saber.controller.PcController.printPcAsTable;
import static com.atmin.saber.util.CyberColors.*;

public class PcManagementMenu {
    private static PcController pcController;

    private static final RoomDao roomDao = new RoomDaoImpl(DBConnection.getInstance());

    public record AddPcInput(String pcName, String roomName, PCStatus status) {
    }

    public record EditPcInput(int pcId, String newName, String newRoomName, PCStatus newStatus) {
    }

    public record DeletePcInput(int pcId, String confirm) {
    }

    private PcManagementMenu() {
    }

    public static void start(Scanner scanner) {
        if (pcController == null) {
            pcController = PcController.createDefault();
        }

        while (true) {
            System.out.println(CYAN + BOLD + "\n  === PC MANAGEMENT SYSTEM ===" + RESET);
            System.out.println("\t1. View PC List");
            System.out.println("\t5. View Room List");
            System.out.println("\t6. View PCs by Room");
            System.out.println("\t2. Add New PC");
            System.out.println("\t3. Edit PC Info");
            System.out.println("\t4. Delete PC");
            System.out.println("\t0. Back to Admin System");
            System.out.print(GREEN + "  ➤ Select an option: " + RESET);

            String input = promptMenuChoice(scanner);

            switch (input) {
                case "1" -> {
                    pcController.listPcs();
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "5" -> {
                    showRooms();
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "6" -> {
                    viewPcsByRoom(scanner);
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "2" -> {
                    AddPcInput addInput = promptAddPc(scanner);
                    pcController.addPc(addInput);
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "3" -> {
                    EditPcInput editInput = promptEditPc(scanner);
                    pcController.editPc(editInput);
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "4" -> {
                    DeletePcInput deleteInput = promptDeletePc(scanner);
                    pcController.deletePc(deleteInput);
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "0" -> {
                    return;
                }
            }
        }
    }

    private static void showRooms() {
        try {
            List<Room> rooms = roomDao.findAll();
            if (rooms.isEmpty()) {
                System.out.println("\tNo rooms found.");
                return;
            }
            System.out.println("\n+------+----------------------+" );
            System.out.printf("| %-4s | %-20s |%n", "ID", "ROOM NAME");
            System.out.println("+------+----------------------+" );
            for (Room r : rooms) {
                System.out.printf("| %-4d | %-20s |%n", r.getRoomId(), r.getRoomName());
            }
            System.out.println("+------+----------------------+" );
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to load rooms: " + ex.getMessage());
        }
    }

    private static void viewPcsByRoom(Scanner scanner) {
        List<Room> rooms;
        try {
            rooms = roomDao.findAll();
        } catch (RuntimeException ex) {
            System.out.println("\tFailed to load rooms: " + ex.getMessage());
            return;
        }
        if (rooms.isEmpty()) {
            System.out.println("\tNo rooms found.");
            return;
        }
        System.out.println("\nSelect room:");
        for (int i = 0; i < rooms.size(); i++) {
            System.out.printf("\t%d. %s%n", i + 1, rooms.get(i).getRoomName());
        }
        System.out.println("\t0. Cancel");
        System.out.print(GREEN + "  ➤ Your choice: " + RESET);

        int idx;
        try {
            idx = Integer.parseInt(ConsoleInput.readLineOrThrow(scanner).trim());
        } catch (Exception ex) {
            System.out.println("\tInvalid input.");
            return;
        }
        if (idx == 0) return;
        if (idx < 1 || idx > rooms.size()) {
            System.out.println("\tInvalid choice.");
            return;
        }
        Room selected = rooms.get(idx - 1);
        System.out.println("\n=== PCS IN ROOM: " + selected.getRoomName() + " ===");
        pcController.listPcsByRoomId(selected.getRoomId());
    }

    private static String promptMenuChoice(Scanner scanner) {
        while (true) {
                if (!scanner.hasNextLine()) {
                    System.out.println("\n[EOF] No more input. Returning...");
                    return "0";
                }
                String input = scanner.nextLine().trim();
            switch (input) {
                case "0", "1", "2", "3", "4", "5", "6" -> {
                    return input;
                }
                default -> {
                    System.out.print("\tInvalid choice.\n");
                    System.out.print(GREEN + "  ➤ Select an option: " + RESET);
                }
            }
        }
    }

    private static AddPcInput promptAddPc(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === ADD NEW PC ===" + RESET);
        String pcName = ConsoleInput.readNonEmpty(scanner, "\tPC Name: ", "\tPC Name cannot be empty.").trim();
        String roomName = promptRoomName(scanner, false);
        PCStatus status = promptStatus(scanner, false);
        return new AddPcInput(pcName, roomName, status);
    }

    private static EditPcInput promptEditPc(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === EDIT PC INFOR ===" + RESET);
        int pcId = promptPositiveInt(scanner, "\tEnter PC ID to edit: ");

        // Display current PC information first
        PC currentPc = pcController.getPcService().getById(pcId).orElse(null);
        if (currentPc == null || currentPc.getStatus() == PCStatus.DELETED) {
            System.out.println("\tError: PC ID does not exist.");
            return new EditPcInput(pcId, null, null, null);
        }

        System.out.println("\tCurrent PC Information:");
        printPcAsTable(currentPc);
        System.out.println();

        System.out.print("\tNew PC Name (Enter to keep current): ");
        String newName = scanner.nextLine().trim();
        String newRoomName = promptRoomName(scanner, true);
        PCStatus newStatus = promptStatus(scanner, true);
        return new EditPcInput(pcId, newName, newRoomName, newStatus);
    }

    private static DeletePcInput promptDeletePc(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === DELETE PC ===" + RESET);
        int pcId = promptPositiveInt(scanner, "\tEnter PC ID to delete: ");
        PC currentPc = pcController.getPcService().getById(pcId).orElse(null);

        if (currentPc == null || currentPc.getStatus() == PCStatus.DELETED) {
            System.out.println("\tError: PC ID does not exist.");
            return new DeletePcInput(pcId, null);
        }
        System.out.println("\tYou are about to delete this PC:");
        printPcAsTable(currentPc);
        System.out.println();
        String confirm = promptYesNo(scanner);
        return new DeletePcInput(pcId, confirm);
    }

    private static int promptPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            int value = ConsoleInput.readInt(scanner, prompt, "\tPC ID must be a positive number.");
            if (value > 0) {
                return value;
            }
            System.out.println("\tPC ID must be a positive number.");
        }
    }

    private static String promptYesNo(Scanner scanner) {
        while (true) {
            System.out.print("\tConfirm deletion? (Y/N): ");
                if (!scanner.hasNextLine()) return "N";
                String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("N")) {
                return input;
            }
            System.out.println("\tInvalid choice. Please enter Y or N.");
        }
    }

    private static String promptRoomName(Scanner scanner, boolean allowKeepCurrent) {
        while (true) {
            List<Room> rooms;
            try {
                rooms = roomDao.findAll();
            } catch (RuntimeException ex) {
                System.out.println("\tFailed to load rooms: " + ex.getMessage());
                if (allowKeepCurrent) {
                    return "";
                }
                continue;
            }

            System.out.println("\tSelect Room" + (allowKeepCurrent ? " (Enter to keep current)" : ":"));
            for (int i = 0; i < rooms.size(); i++) {
                System.out.println("\t" + (i + 1) + ". " + rooms.get(i).getRoomName());
            }
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);

            String input = scanner.nextLine().trim();
            if (allowKeepCurrent && input.isEmpty()) {
                return "";
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice < 1 || choice > rooms.size()) {
                    System.out.println("\tInvalid choice.");
                    continue;
                }
                return rooms.get(choice - 1).getRoomName();
            } catch (NumberFormatException e) {
                System.out.println("\tPlease enter a number.");
            }
        }
    }

    private static PCStatus promptStatus(Scanner scanner, boolean allowKeepCurrent) {
        while (true) {
            System.out.println("\tEnter status" + (allowKeepCurrent ? " (Enter to keep current)" : ":"));
            System.out.println("\t1. AVAILABLE");
            System.out.println("\t2. IN_USE");
            System.out.println("\t3. MAINTENANCE");
            System.out.println("\t4. BOOKED");
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);

            String input = scanner.nextLine().trim();
            if (allowKeepCurrent && input.isEmpty()) {
                return null;
            }

            switch (input) {
                case "1" -> {
                    return PCStatus.AVAILABLE;
                }
                case "2" -> {
                    return PCStatus.IN_USE;
                }
                case "3" -> {
                    return PCStatus.MAINTENANCE;
                }
                case "4" -> {
                    return PCStatus.BOOKED;
                }
                default -> System.out.println("\tInvalid choice. Please select 1-4.");
            }
        }
    }
}

