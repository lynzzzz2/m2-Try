package org.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    // All statics are now properly initialised in the static block at the bottom.
    private static final Scanner scanner;
    private static final Repository repo;
    private static final BookingManager bookingManager;
    private static final AuthenticationService authService;

    // Mutable state — not final
    private static int    loggedInUserId;
    private static String loggedInUserName;

    // ─────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────

    public static void main(String[] args) {
        repo.connect();
        repo.createTables();
        repo.migrateBookingsTable();
        repo.seedRooms();

        if (!authService.adminPasswordExists()) {
            authService.setAdminPassword("admin123");
            System.out.println("Default admin password initialized.");
        }

        int choice;
        do {
            System.out.println("\n=======================================");
            System.out.println("  Welcome to Co-working Space Hub!");
            System.out.println("=======================================");
            System.out.println("1. I am a User");
            System.out.println("2. I am an Admin");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = getValidChoice(0, 2);
            switch (choice) {
                case 0:
                    System.out.println("Thank you for using Co-working Space Hub. Goodbye!");
                    break;
                case 1:
                    userMenu();
                    break;
                case 2:
                    adminLogin();
                    break;
            }
        } while (choice != 0);

        repo.close();
    }

    // ─────────────────────────────────────────
    // INPUT HELPER
    // ─────────────────────────────────────────

    private static int getValidChoice(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine().trim());
                if (input >= min && input <= max) {
                    return input;
                }
                System.out.println("Invalid number. Please enter a number between " + min + " and " + max + ".");
                System.out.print("Enter choice: ");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                System.out.print("Enter choice: ");
            }
        }
    }

    // ─────────────────────────────────────────
    // USER MENU
    // ─────────────────────────────────────────

    private static void userMenu() {
        int choice;
        do {
            if (loggedInUserId == -1) {
                // ── NOT LOGGED IN: show register / login ──
                System.out.println("\n=======================================");
                System.out.println("  Welcome to Co-working Space Hub!");
                System.out.println("=======================================");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("0. Back");
                System.out.print("Enter choice: ");
                choice = getValidChoice(0, 2);

                switch (choice) {
                    case 0:
                        return;

                    case 1:
                        System.out.println("\n===== REGISTER =====");
                        System.out.print("Name: ");
                        String regName = scanner.nextLine();

                        String regEmail;
                        while (true) {
                            System.out.print("Email: ");
                            regEmail = scanner.nextLine();
                            if (authService.isValidGmail(regEmail)) break;
                            System.out.println("Warning: " + authService.getEmailWarning(regEmail));
                        }

                        System.out.print("Password: ");
                        String regPassword = scanner.nextLine();

                        System.out.println("\n--- Registration Summary ---");
                        System.out.println("Name     : " + regName);
                        System.out.println("Email    : " + regEmail);
                        System.out.println("Password : " + "*".repeat(regPassword.length()));
                        System.out.println("----------------------------");
                        System.out.println("1. Proceed");
                        System.out.println("2. Cancel");
                        System.out.print("Enter choice: ");
                        if (getValidChoice(1, 2) == 1) {
                            authService.registerUser(regName, regEmail, regPassword, "customer");
                        } else {
                            System.out.println("Registration cancelled. Returning to menu...");
                        }
                        break;

                    case 2:
                        System.out.println("\n===== LOGIN =====");

                        String loginEmail;
                        while (true) {
                            System.out.print("Email: ");
                            loginEmail = scanner.nextLine();
                            if (authService.isValidGmail(loginEmail)) break;
                            System.out.println("Warning: " + authService.getEmailWarning(loginEmail));
                        }

                        System.out.print("Password: ");
                        String loginPassword = scanner.nextLine();
                        int result = authService.loginUser(loginEmail, loginPassword);
                        if (result != -1) {
                            loggedInUserId   = result;
                            loggedInUserName = getNameFromDb(loginEmail);
                        }
                        break;

                    default:
                        choice = -1; // keep loop going
                }

            } else {
                // ── LOGGED IN: show user actions ──
                System.out.println("\n===== USER MENU =====");
                System.out.println("Logged in as: " + loggedInUserName);
                System.out.println("1. View Available Rooms");
                System.out.println("2. Book Workspace");
                System.out.println("3. My Bookings");
                System.out.println("4. Cancel Booking");
                System.out.println("5. Log Out");
                System.out.print("Enter choice: ");
                choice = getValidChoice(1, 5);

                switch (choice) {
                    case 1:
                        repo.printRoomsTable();
                        break;
                    case 2:
                        bookingManager.bookWorkspace(loggedInUserId);
                        break;
                    case 3:
                        bookingManager.viewMyBookings(loggedInUserId);
                        break;
                    case 4:
                        bookingManager.cancelBooking(loggedInUserId);
                        break;
                    case 5:
                        System.out.println("Logged out successfully. Goodbye, " + loggedInUserName + "!");
                        loggedInUserId   = -1;
                        loggedInUserName = "";
                        choice = 0; // exit logged-in sub-loop → back to top
                        break;
                }
            }
        } while (choice != 0);
    }

    // ─────────────────────────────────────────
    // FETCH NAME FROM DB
    // ─────────────────────────────────────────

    private static String getNameFromDb(String email) {
        ResultSet rs = repo.getUserByEmail(email);
        try {
            if (rs != null && rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user name: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) { }
        }
        return "User";
    }

    // ─────────────────────────────────────────
    // ADMIN MENU
    // ─────────────────────────────────────────

    private static void adminLogin() {
        System.out.print("\nEnter Admin Password: ");
        String input = scanner.nextLine();
        if (authService.verifyAdminPassword(input)) {
            System.out.println("Access granted. Welcome, Admin!");
            adminMenu();
        } else {
            System.out.println("Incorrect password. Access denied.");
        }
    }

    private static void adminMenu() {
        int choice;
        do {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. View All Bookings");
            System.out.println("2. View All Users");
            System.out.println("3. View All Rooms");
            System.out.println("4. Add Room");
            System.out.println("5. Cancel Any Booking");
            System.out.println("6. Change Admin Password");
            System.out.println("7. Income Statement");
            System.out.println("0. Log out Admin");
            System.out.print("Enter choice: ");
            choice = getValidChoice(0, 7);

            switch (choice) {
                case 0:
                    System.out.println("Returning to main menu...");
                    break;
                case 1:
                    repo.printBookingsTable();
                    break;
                case 2:
                    repo.printUsersTable();
                    break;
                case 3:
                    repo.printRoomsTable();
                    break;
                case 4:
                    addRoom();
                    break;
                case 5:
                    bookingManager.cancelBookingAdmin();
                    break;
                case 6:
                    changeAdminPassword();
                    break;
                case 7:
                    repo.printIncomeStatement();
                    break;
            }
        } while (choice != 0);
    }

    private static void addRoom() {
        System.out.println("\n===== ADD ROOM =====");
        System.out.print("Room Name: ");
        String roomName = scanner.nextLine();
        System.out.print("Room Type: ");
        String roomType = scanner.nextLine();

        int capacity = 0;
        while (true) {
            System.out.print("Capacity: ");
            try {
                capacity = Integer.parseInt(scanner.nextLine().trim());
                if (capacity > 0) break;
                System.out.println("Capacity must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }

        double pricePerHour = 0.0;
        while (true) {
            System.out.print("Price Per Hour (PHP): ");
            try {
                pricePerHour = Double.parseDouble(scanner.nextLine().trim());
                if (pricePerHour >= 0.0) break;
                System.out.println("Price cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid price.");
            }
        }

        System.out.println("\n--- Room Summary ---");
        System.out.println("Room Name  : " + roomName);
        System.out.println("Room Type  : " + roomType);
        System.out.println("Capacity   : " + capacity);
        System.out.printf("Price/Hour : PHP %.2f%n", pricePerHour);
        System.out.println("--------------------");
        System.out.println("1. Proceed");
        System.out.println("2. Cancel");
        System.out.print("Enter choice: ");

        if (getValidChoice(1, 2) == 1) {
            repo.insertRoom(roomName, roomType, capacity, pricePerHour, "available");
            System.out.println("Room added successfully!");
        } else {
            System.out.println("Room addition cancelled. Returning to admin menu...");
        }
    }

    private static void changeAdminPassword() {
        System.out.print("Enter current password: ");
        String current = scanner.nextLine();
        if (authService.verifyAdminPassword(current)) {
            System.out.print("Enter new password: ");
            String newPass = scanner.nextLine();
            System.out.print("Confirm new password: ");
            String confirm = scanner.nextLine();
            if (newPass.equals(confirm)) {
                authService.setAdminPassword(newPass);
            } else {
                System.out.println("Passwords do not match. Password not changed.");
            }
        } else {
            System.out.println("Current password incorrect.");
        }
    }

    // ─────────────────────────────────────────
    // STATIC INITIALISER — fixes NullPointerException on startup
    // ─────────────────────────────────────────

    static {
        scanner          = new Scanner(System.in);
        repo             = new Repository();
        bookingManager   = new BookingManager(repo, scanner);
        authService      = new AuthenticationService(repo);
        loggedInUserId   = -1;
        loggedInUserName = "";
    }
}
