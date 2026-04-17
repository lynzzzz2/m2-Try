package org.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner;
    private static final Repository repo;
    private static final BookingManager bookingManager;
    private static final AuthenticationService authService;
    private static int loggedInUserId;
    private static String loggedInUserName;
    private static final String ADMIN_PASSWORD = "admin123";

    public Main() {
    }

    public static void main(String[] args) {
        repo.connect();
        repo.createTables();
        repo.migrateBookingsTable();
        repo.seedRooms();

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
            }
        } while(choice != 0);

        repo.close();
    }

    private static int getValidChoice(int min, int max) {
        while(true) {
            try {
                int input = Integer.parseInt(scanner.nextLine().trim());
                if (input >= min && input <= max) {
                    return input;
                }

                System.out.println("Invalid number. Please enter a number between " + min + " and " + max + ".");
                System.out.print("Enter choice: ");
            } catch (NumberFormatException var3) {
                System.out.println("Invalid number. Please enter a valid number.");
                System.out.print("Enter choice: ");
            }
        }
    }

    private static void userMenu() {
        int choice;
        label53:
        do {
            if (loggedInUserId == -1) {
                System.out.println("\n=======================================");
                System.out.println("  Welcome to Co-working Space Hub!");
                System.out.println("=======================================");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.print("Enter choice: ");
                choice = getValidChoice(1, 2);
                switch (choice) {
                    case 1:
                        System.out.println("\n===== REGISTER =====");
                        System.out.print("Name: ");
                        String name = scanner.nextLine();

                        while(true) {
                            System.out.print("Email: ");
                            String email = scanner.nextLine();
                            if (authService.isValidGmail(email)) {
                                System.out.print("Password: ");
                                String password = scanner.nextLine();
                                System.out.println("\n--- Registration Summary ---");
                                System.out.println("Name     : " + name);
                                System.out.println("Email    : " + email);
                                System.out.println("Password : " + "*".repeat(password.length()));
                                System.out.println("----------------------------");
                                System.out.println("1. Proceed");
                                System.out.println("2. Cancel");
                                System.out.print("Enter choice: ");
                                int confirmReg = getValidChoice(1, 2);
                                if (confirmReg == 1) {
                                    authService.registerUser(name, email, password, "customer");
                                } else {
                                    System.out.println("Registration cancelled. Returning to menu...");
                                }
                                continue label53;
                            }

                            System.out.println("Warning: " + authService.getEmailWarning(email));
                        }
                    case 2:
                        System.out.println("\n===== LOGIN =====");

                        while(true) {
                            System.out.print("Email: ");
                            String email = scanner.nextLine();
                            if (authService.isValidGmail(email)) {
                                System.out.print("Password: ");
                                String password = scanner.nextLine();
                                int result = authService.loginUser(email, password);
                                if (result != -1) {
                                    loggedInUserId = result;
                                    loggedInUserName = getNameFromDb(email);
                                }
                                break;
                            }

                            System.out.println("Warning: " + authService.getEmailWarning(email));
                        }
                }
            } else {
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
                        loggedInUserId = -1;
                        loggedInUserName = "";
                        choice = 0;
                }
            }
        } while(choice != 0);

    }

    private static String getNameFromDb(String email) {
        ResultSet rs = repo.getUserByEmail(email);

        try {
            if (rs != null && rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user name: " + e.getMessage());
        }

        return "User";
    }

    private static void adminLogin() {
        System.out.print("\nEnter Admin Password: ");
        String input = scanner.nextLine();
        if (input.equals("admin123")) {
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
            System.out.println("0. Back to Main Menu");
            System.out.print("Enter choice: ");
            choice = getValidChoice(0, 5);
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
                    System.out.println("\n===== ADD ROOM =====");
                    System.out.print("Room Name: ");
                    String roomName = scanner.nextLine();
                    System.out.print("Room Type: ");
                    String roomType = scanner.nextLine();
                    int capacity = 0;

                    while(true) {
                        System.out.print("Capacity: ");

                        try {
                            capacity = Integer.parseInt(scanner.nextLine().trim());
                            if (capacity > 0) {
                                break;
                            }

                            System.out.println("Capacity must be greater than 0.");
                        } catch (NumberFormatException var8) {
                            System.out.println("Invalid input. Please enter a valid number.");
                        }
                    }

                    double pricePerHour = (double)0.0F;

                    while(true) {
                        System.out.print("Price Per Hour (PHP): ");

                        try {
                            pricePerHour = Double.parseDouble(scanner.nextLine().trim());
                            if (pricePerHour >= (double)0.0F) {
                                break;
                            }

                            System.out.println("Price cannot be negative.");
                        } catch (NumberFormatException var7) {
                            System.out.println("Invalid input. Please enter a valid price.");
                        }
                    }

                    System.out.println("\n--- Room Summary ---");
                    System.out.println("Room Name    : " + roomName);
                    System.out.println("Room Type    : " + roomType);
                    System.out.println("Capacity     : " + capacity);
                    System.out.printf("Price/Hour   : PHP %.2f%n", pricePerHour);
                    System.out.println("--------------------");
                    System.out.println("1. Proceed");
                    System.out.println("2. Cancel");
                    System.out.print("Enter choice: ");
                    int confirmRoom = getValidChoice(1, 2);
                    if (confirmRoom == 1) {
                        repo.insertRoom(roomName, roomType, capacity, pricePerHour, "available");
                        System.out.println("Room added successfully!");
                    } else {
                        System.out.println("Room addition cancelled. Returning to admin menu...");
                    }
                    break;
                case 5:
                    bookingManager.cancelBookingAdmin();
            }
        } while(choice != 0);

    }

    static {
        scanner = new Scanner(System.in);
        repo = new Repository();
        bookingManager = new BookingManager(repo);
        authService = new AuthenticationService(repo);
        loggedInUserId = -1;
        loggedInUserName = "";
    }
}
