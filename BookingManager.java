package org.example;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class BookingManager {

    private final Scanner scanner = new Scanner(System.in);
    private final Repository repo;

    public BookingManager(Repository repo) {
        this.repo = repo;
    }

    public void bookWorkspace(int userId) {
        System.out.println("\n===== BOOK WORKSPACE =====");

        System.out.print("Enter Room ID: ");
        int roomId = scanner.nextInt();
        scanner.nextLine(); // clear buffer

        System.out.print("Enter Date (e.g. 2026-04-01): ");
        String date = scanner.nextLine();

        System.out.print("Enter Time (e.g. 13:00): ");
        String time = scanner.nextLine();

        repo.updateRoomAvailability(roomId, "occupied");

        repo.insertBooking(userId, roomId, date, time); // matches your Repository
        System.out.println("Workspace booked successfully!");
    }

    public void cancelBooking() {
        System.out.println("\n===== CANCEL BOOKING =====");

        System.out.print("Enter Booking ID: ");
        int bookingId = scanner.nextInt();

        int roomId = repo.getRoomIdByBooking(bookingId);

        if (roomId == -1) {
            System.out.println("Booking not found.");
            return;
        }

        repo.cancelBooking(bookingId);

        repo.updateRoomAvailability(roomId, "available");

        System.out.println("Booking cancelled and room is now available.");
    }

    public void viewAllBookings() {
        System.out.println("\n===== ALL BOOKINGS =====");

        ResultSet rs = repo.getAllBookings(); // correct method name
        try {
            boolean hasBookings = false;
            while (rs != null && rs.next()) {
                hasBookings = true;
                new Booking(
                        rs.getInt("bookingId"),
                        rs.getInt("userId"),
                        rs.getInt("roomId"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getString("status")
                ).display(); // uses the Booking class now
            }
            if (!hasBookings) System.out.println("No bookings available.");
        } catch (SQLException e) {
            System.out.println("Error displaying bookings: " + e.getMessage());
        }
    }
}