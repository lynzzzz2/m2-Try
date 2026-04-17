package org.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BookingManager {

    private final Scanner scanner = new Scanner(System.in);
    private final Repository repo;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BookingManager(Repository repo) {
        this.repo = repo;
    }

    // ─────────────────────────────────────────
    // HELPER: get list of available room IDs
    // ─────────────────────────────────────────

    private List<Integer> getAvailableRoomIds() {
        List<Integer> ids = new ArrayList<>();
        ResultSet rs = repo.getAvailableRooms();
        try {
            while (rs != null && rs.next()) {
                ids.add(rs.getInt("roomId"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching available rooms: " + e.getMessage());
        }
        return ids;
    }

    // ─────────────────────────────────────────
    // HELPER: get list of active booking IDs by user
    // ─────────────────────────────────────────

    private List<Integer> getUserActiveBookingIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        ResultSet rs = repo.getBookingsByUser(userId);
        try {
            while (rs != null && rs.next()) {
                if (rs.getString("status").equals("active")) {
                    ids.add(rs.getInt("bookingId"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user bookings: " + e.getMessage());
        }
        return ids;
    }

    // ─────────────────────────────────────────
    // HELPER: validated int input within range
    // ─────────────────────────────────────────

    private int getValidChoice(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine().trim());
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.println("Invalid number. Please enter a number between " + min + " and " + max + ".");
                    System.out.print("Enter choice: ");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid number.");
                System.out.print("Enter choice: ");
            }
        }
    }

    // ─────────────────────────────────────────
    // HELPER: parse time string to minutes
    // ─────────────────────────────────────────

    private int parseTimeToMinutes(String time) {
        try {
            String[] parts = time.trim().split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }

    // ─────────────────────────────────────────
    // HELPER: validate date string
    // ─────────────────────────────────────────

    private String getValidDate() {
        LocalDate today = LocalDate.now();
        while (true) {
            System.out.print("Enter Date (e.g. 2026-04-01): ");
            String input = scanner.nextLine().trim();
            try {
                LocalDate date = LocalDate.parse(input, DATE_FORMAT);
                if (date.isBefore(today)) {
                    System.out.println("Invalid date. You cannot book a date in the past. Please enter a valid future date.");
                } else {
                    return input;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD (e.g. 2026-04-01).");
            }
        }
    }

    // ─────────────────────────────────────────
    // HELPER: check if time slot conflicts
    // ─────────────────────────────────────────

    private boolean hasTimeConflict(int roomId, String date, int startMinutes, int durationMinutes) {
        int newEnd = startMinutes + durationMinutes;
        ResultSet rs = repo.getBookingsByRoom(roomId);
        try {
            while (rs != null && rs.next()) {
                if (!rs.getString("date").equals(date)) continue;
                int existingStart = parseTimeToMinutes(rs.getString("time"));
                int existingEnd = existingStart + rs.getInt("duration");
                if (startMinutes < existingEnd && newEnd > existingStart) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking time conflict: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────
    // BOOK WORKSPACE
    // ─────────────────────────────────────────

    public void bookWorkspace(int userId) {
        System.out.println("\n===== BOOK WORKSPACE =====");

        // ── SHOW AVAILABLE ROOMS TABLE ──
        repo.printRoomsTable();

        List<Integer> availableIds = getAvailableRoomIds();
        if (availableIds.isEmpty()) {
            System.out.println("No available rooms to book at the moment.");
            return;
        }

        // ── GET ROOM ID ──
        int roomId;
        while (true) {
            System.out.print("Enter Room ID: ");
            try {
                roomId = Integer.parseInt(scanner.nextLine().trim());
                if (availableIds.contains(roomId)) break;
                System.out.println("Room ID " + roomId + " not found in available rooms. Please enter a valid Room ID.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid Room ID.");
            }
        }

        // ── GET PRICE PER HOUR ──
        double pricePerHour = repo.getRoomPricePerHour(roomId);

        // ── GET DATE ──
        String date = getValidDate();

        // ── GET START TIME ──
        String time;
        int startMinutes;
        while (true) {
            System.out.print("Enter Start Time (e.g. 13:00): ");
            time = scanner.nextLine();
            startMinutes = parseTimeToMinutes(time);
            if (startMinutes != -1) break;
            System.out.println("Invalid time format. Please use HH:MM (e.g. 13:00).");
        }

        // ── GET DURATION ──
        int duration;
        while (true) {
            System.out.print("Enter Duration in minutes (e.g. 60 for 1 hour): ");
            try {
                duration = Integer.parseInt(scanner.nextLine().trim());
                if (duration > 0) break;
                System.out.println("Duration must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number of minutes.");
            }
        }

        // ── CHECK TIME CONFLICT ──
        if (hasTimeConflict(roomId, date, startMinutes, duration)) {
            System.out.println("The booking time is not available. Please choose a different time or duration.");
            return;
        }

        // ── COMPUTE PRICE ──
        double hours = duration / 60.0;
        double totalPrice = pricePerHour * hours;

        int endMinutes = startMinutes + duration;
        String endTime = String.format("%02d:%02d", endMinutes / 60, endMinutes % 60);

        // ── CONFIRMATION ──
        System.out.println("\n--- Booking Summary ---");
        System.out.printf("Room ID      : %d%n",     roomId);
        System.out.printf("Date         : %s%n",     date);
        System.out.printf("Start Time   : %s%n",     time);
        System.out.printf("End Time     : %s%n",     endTime);
        System.out.printf("Duration     : %d minutes%n", duration);
        System.out.printf("Price/Hour   : PHP %.2f%n", pricePerHour);
        System.out.printf("Total Price  : PHP %.2f%n", totalPrice);
        System.out.println("-----------------------");
        System.out.println("1. Proceed");
        System.out.println("2. Cancel");
        System.out.print("Enter choice: ");
        int confirm = getValidChoice(1, 2);

        if (confirm == 1) {
            repo.insertBooking(userId, roomId, date, time, duration, totalPrice);
            System.out.println("Booking successful!");
            System.out.printf("Total amount charged: PHP %.2f%n", totalPrice);
        } else {
            System.out.println("Booking cancelled. Returning to menu...");
        }
    }

    // ─────────────────────────────────────────
    // VIEW MY BOOKINGS
    // ─────────────────────────────────────────

    public void viewMyBookings(int userId) {
        System.out.println("\n===== MY BOOKINGS =====");
        ResultSet rs = repo.getBookingsByUser(userId);
        try {
            boolean found = false;
            while (rs != null && rs.next()) {
                found = true;
                int startMins = parseTimeToMinutes(rs.getString("time"));
                int dur = rs.getInt("duration");
                int endMins = startMins + dur;
                String endTime = String.format("%02d:%02d", endMins / 60, endMins % 60);

                System.out.println("Booking ID   : " + rs.getInt("bookingId"));
                System.out.println("Room ID      : " + rs.getInt("roomId"));
                System.out.println("Date         : " + rs.getString("date"));
                System.out.println("Start Time   : " + rs.getString("time"));
                System.out.println("End Time     : " + endTime);
                System.out.println("Duration     : " + dur + " minutes");
                System.out.printf("Total Price  : PHP %.2f%n", rs.getDouble("totalPrice"));
                System.out.println("Status       : " + rs.getString("status"));
                System.out.println("---------------------------");
            }
            if (!found) System.out.println("You have no bookings yet.");
        } catch (SQLException e) {
            System.out.println("Error displaying your bookings: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // CANCEL BOOKING (user)
    // ─────────────────────────────────────────

    public void cancelBooking(int userId) {
        System.out.println("\n===== CANCEL BOOKING =====");

        List<Integer> userBookingIds = getUserActiveBookingIds(userId);
        if (userBookingIds.isEmpty()) {
            System.out.println("You have no active bookings to cancel.");
            return;
        }

        System.out.println("Your active Booking IDs: " + userBookingIds);
        System.out.println("1. Cancel a Booking");
        System.out.println("2. Back to User Menu");
        System.out.print("Enter choice: ");
        int choice = getValidChoice(1, 2);

        if (choice == 2) {
            System.out.println("Returning to user menu...");
            return;
        }

        int bookingId;
        while (true) {
            System.out.print("Enter Booking ID: ");
            try {
                bookingId = Integer.parseInt(scanner.nextLine().trim());
                if (userBookingIds.contains(bookingId)) break;
                System.out.println("Booking ID " + bookingId + " is invalid. Please enter a Booking ID from your bookings.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid Booking ID.");
            }
        }

        repo.cancelBooking(bookingId);
        System.out.println("Booking cancelled successfully.");
    }

    // ─────────────────────────────────────────
    // CANCEL BOOKING (admin)
    // ─────────────────────────────────────────

    public void cancelBookingAdmin() {
        System.out.println("\n===== CANCEL BOOKING =====");

        System.out.print("Enter Booking ID: ");
        int bookingId;
        try {
            bookingId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        int roomId = repo.getRoomIdByBooking(bookingId);
        if (roomId == -1) {
            System.out.println("Booking not found.");
            return;
        }

        repo.cancelBooking(bookingId);
        System.out.println("Booking cancelled successfully.");
    }

    // ─────────────────────────────────────────
    // VIEW ALL BOOKINGS (admin)
    // ─────────────────────────────────────────

    public void viewAllBookings() {
        repo.printBookingsTable();
    }
}