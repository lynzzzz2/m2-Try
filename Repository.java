package org.example;

import java.sql.*;

public class Repository {

    private static final String DB_URL = "jdbc:sqlite:C:\\Users\\Administrator\\IdeaProjects\\CoworkingSpaceHub.db";
    private Connection connection;

    // ─────────────────────────────────────────
    // CONNECTION METHODS
    // ─────────────────────────────────────────

    public void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database successfully.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // CREATE TABLES
    // ─────────────────────────────────────────

    public void createTables() {
        String createUserTable = """
                CREATE TABLE IF NOT EXISTS users (
                    userId    INTEGER PRIMARY KEY AUTOINCREMENT,
                    name      TEXT NOT NULL,
                    email     TEXT NOT NULL UNIQUE,
                    password  TEXT NOT NULL,
                    role      TEXT NOT NULL DEFAULT 'customer'
                );
                """;

        String createRoomTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    roomId       INTEGER PRIMARY KEY AUTOINCREMENT,
                    roomName     TEXT NOT NULL,
                    roomType     TEXT NOT NULL,
                    capacity     INTEGER NOT NULL,
                    pricePerHour REAL NOT NULL DEFAULT 0.0,
                    availability TEXT NOT NULL DEFAULT 'available'
                );
                """;

        String createBookingTable = """
                CREATE TABLE IF NOT EXISTS bookings (
                    bookingId    INTEGER PRIMARY KEY AUTOINCREMENT,
                    userId       INTEGER NOT NULL,
                    roomId       INTEGER NOT NULL,
                    date         TEXT NOT NULL,
                    time         TEXT NOT NULL,
                    duration     INTEGER NOT NULL DEFAULT 60,
                    totalPrice   REAL NOT NULL DEFAULT 0.0,
                    status       TEXT NOT NULL DEFAULT 'active',
                    FOREIGN KEY (userId) REFERENCES users(userId),
                    FOREIGN KEY (roomId) REFERENCES rooms(roomId)
                );
                """;

        String createConfigTable = """
                CREATE TABLE IF NOT EXISTS app_config (
                    key   TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUserTable);
            stmt.execute(createRoomTable);
            stmt.execute(createBookingTable);
            stmt.execute(createConfigTable);
            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // MIGRATIONS
    // ─────────────────────────────────────────

    public void migrateBookingsTable() {
        addColumnIfMissing("bookings", "duration",   "INTEGER NOT NULL DEFAULT 60");
        addColumnIfMissing("bookings", "totalPrice", "REAL NOT NULL DEFAULT 0.0");
        addColumnIfMissing("rooms",    "pricePerHour", "REAL NOT NULL DEFAULT 0.0");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        String pragma = "PRAGMA table_info(" + table + ")";
        boolean exists = false;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(pragma)) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) {
                    exists = true;
                    break;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking schema for " + table + ": " + e.getMessage());
            return;
        }

        if (!exists) {
            String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(alter);
                System.out.println("Migration successful: '" + column + "' column added to " + table + ".");
            } catch (SQLException e) {
                System.out.println("Migration failed for " + column + ": " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────
    // SEED ROOMS
    // ─────────────────────────────────────────

    public void seedRooms() {
        String check = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {

                // Hot Desks
                insertRoom("Hot Desk A",       "Hot Desk",       1,  50.0,  "available");
                insertRoom("Hot Desk B",       "Hot Desk",       1,  50.0,  "available");
                insertRoom("Hot Desk C",       "Hot Desk",       1,  50.0,  "available");

                // Meeting Rooms
                insertRoom("Meeting Room 1",   "Meeting Room",   6,  150.0, "available");
                insertRoom("Meeting Room 2",   "Meeting Room",   10, 200.0, "available");
                insertRoom("Meeting Room 3",   "Meeting Room",   4,  120.0, "available");

                // Private Offices
                insertRoom("Private Office 1", "Private Office", 2,  100.0, "available");
                insertRoom("Private Office 2", "Private Office", 4,  180.0, "available");
                insertRoom("Private Office 3", "Private Office", 6,  250.0, "available");

                // Training / Event
                insertRoom("Training Room A",  "Training Room",  20, 300.0, "available");
                insertRoom("Event Hall",       "Event Hall",     50, 500.0, "available");

                System.out.println("Sample rooms added.");
            }
        } catch (SQLException e) {
            System.out.println("Error seeding rooms: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // USER METHODS
    // ─────────────────────────────────────────

    public void insertUser(String name, String email, String password, String role) {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            System.out.println("User registered successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting user: " + e.getMessage());
        }
    }

    public ResultSet getUsers() {
        String sql = "SELECT * FROM users";
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error retrieving users: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, email);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error retrieving user: " + e.getMessage());
            return null;
        }
    }
    // Add to the USER METHODS section

    public String getAdminPasswordHash() {
        String sql = "SELECT value FROM app_config WHERE key = 'admin_password_hash'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            System.out.println("Error fetching admin password: " + e.getMessage());
        }
        return null;
    }

    public void setAdminPasswordHash(String hash) {
        String sql = "INSERT INTO app_config (key, value) VALUES ('admin_password_hash', ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error saving admin password: " + e.getMessage());
        }
    }



    // ─────────────────────────────────────────
    // ROOM METHODS
    // ─────────────────────────────────────────

    public void insertRoom(String roomName, String roomType, int capacity, double pricePerHour, String availability) {
        String sql = "INSERT INTO rooms (roomName, roomType, capacity, pricePerHour, availability) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, roomType);
            pstmt.setInt(3, capacity);
            pstmt.setDouble(4, pricePerHour);
            pstmt.setString(5, availability);
            pstmt.executeUpdate();
            System.out.println("Room added successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting room: " + e.getMessage());
        }
    }

    public ResultSet getAvailableRooms() {
        String sql = "SELECT * FROM rooms WHERE availability = 'available'";
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error retrieving rooms: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getAllRooms() {
        String sql = "SELECT * FROM rooms";
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error retrieving all rooms: " + e.getMessage());
            return null;
        }
    }

    public double getRoomPricePerHour(int roomId) {
        String sql = "SELECT pricePerHour FROM rooms WHERE roomId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("pricePerHour");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving room price: " + e.getMessage());
        }
        return 0.0;
    }

    public void updateRoomAvailability(int roomId, String availability) {
        String sql = "UPDATE rooms SET availability = ? WHERE roomId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, availability);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating room: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // BOOKING METHODS
    // ─────────────────────────────────────────

    public void insertBooking(int userId, int roomId, String date, String time, int duration, double totalPrice) {
        String sql = "INSERT INTO bookings (userId, roomId, date, time, duration, totalPrice, status) VALUES (?, ?, ?, ?, ?, ?, 'active')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, roomId);
            pstmt.setString(3, date);
            pstmt.setString(4, time);
            pstmt.setInt(5, duration);
            pstmt.setDouble(6, totalPrice);
            pstmt.executeUpdate();
            System.out.println("Booking created successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting booking: " + e.getMessage());
        }
    }

    public void cancelBooking(int bookingId) {
        String sql = "UPDATE bookings SET status = 'cancelled' WHERE bookingId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
            System.out.println("Booking cancelled successfully.");
        } catch (SQLException e) {
            System.out.println("Error cancelling booking: " + e.getMessage());
        }
    }

    public int getRoomIdByBooking(int bookingId) {
        String sql = "SELECT roomId FROM bookings WHERE bookingId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("roomId");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving roomId: " + e.getMessage());
        }
        return -1;
    }

    public ResultSet getBookingsByUser(int userId) {
        String sql = "SELECT * FROM bookings WHERE userId = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error retrieving user bookings: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getAllBookings() {
        String sql = "SELECT * FROM bookings";
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error retrieving all bookings: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getBookingsByRoom(int roomId) {
        String sql = "SELECT * FROM bookings WHERE roomId = ? AND status = 'active'";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, roomId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error retrieving room bookings: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────
    // TABLE DISPLAY METHODS
    // ─────────────────────────────────────────

    public void printUsersTable() {
        String sql = "SELECT * FROM users";
        System.out.println("\n╔══════════╦══════════════════════╦══════════════════════════════╦══════════╗");
        System.out.println(  "║ User ID  ║ Name                 ║ Email                        ║ Role     ║");
        System.out.println(  "╠══════════╬══════════════════════╬══════════════════════════════╬══════════╣");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("║ %-8d ║ %-20s ║ %-28s ║ %-8s ║%n",
                        rs.getInt("userId"),
                        truncate(rs.getString("name"), 20),
                        truncate(rs.getString("email"), 28),
                        truncate(rs.getString("role"), 8));
            }
            if (!found) {
                System.out.println("║                        No users found.                              ║");
            }
        } catch (SQLException e) {
            System.out.println("Error printing users table: " + e.getMessage());
        }
        System.out.println("╚══════════╩══════════════════════╩══════════════════════════════╩══════════╝");
    }

    public void printRoomsTable() {
        String sql = "SELECT * FROM rooms WHERE availability = 'available'";
        System.out.println("\n╔═════════╦══════════════════════╦══════════════════╦══════════╦══════════════════╦═════════════╗");
        System.out.println(  "║ Room ID ║ Room Name            ║ Type             ║ Capacity ║ Price / Hour     ║ Status      ║");
        System.out.println(  "╠═════════╬══════════════════════╬══════════════════╬══════════╬══════════════════╬═════════════╣");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("║ %-7d ║ %-20s ║ %-16s ║ %-8d ║ PHP %-12.2f ║ %-11s ║%n",
                        rs.getInt("roomId"),
                        truncate(rs.getString("roomName"), 20),
                        truncate(rs.getString("roomType"), 16),
                        rs.getInt("capacity"),
                        rs.getDouble("pricePerHour"),
                        truncate(rs.getString("availability"), 11));
            }
            if (!found) {
                System.out.println("║                              No available rooms found.                                      ║");
            }
        } catch (SQLException e) {
            System.out.println("Error printing rooms table: " + e.getMessage());
        }
        System.out.println("╚═════════╩══════════════════════╩══════════════════╩══════════╩══════════════════╩═════════════╝");
    }

    public void printAllRoomsTable() {
        String sql = "SELECT * FROM rooms";
        System.out.println("\n╔═════════╦══════════════════════╦══════════════════╦══════════╦══════════════════╦═════════════╗");
        System.out.println(  "║ Room ID ║ Room Name            ║ Type             ║ Capacity ║ Price / Hour     ║ Status      ║");
        System.out.println(  "╠═════════╬══════════════════════╬══════════════════╬══════════╬══════════════════╬═════════════╣");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("║ %-7d ║ %-20s ║ %-16s ║ %-8d ║ PHP %-12.2f ║ %-11s ║%n",
                        rs.getInt("roomId"),
                        truncate(rs.getString("roomName"), 20),
                        truncate(rs.getString("roomType"), 16),
                        rs.getInt("capacity"),
                        rs.getDouble("pricePerHour"),
                        truncate(rs.getString("availability"), 11));
            }
            if (!found) {
                System.out.println("║                                  No rooms found.                                            ║");
            }
        } catch (SQLException e) {
            System.out.println("Error printing all rooms table: " + e.getMessage());
        }
        System.out.println("╚═════════╩══════════════════════╩══════════════════╩══════════╩══════════════════╩═════════════╝");
    }

    public void printBookingsTable() {
        String sql = "SELECT * FROM bookings";
        System.out.println("\n╔════════════╦═════════╦═════════╦════════════╦══════════╦══════════╦══════════╦═════════════════╦══════════╗");
        System.out.println(  "║ Booking ID ║ User ID ║ Room ID ║ Date       ║ Start    ║ End      ║ Duration ║ Total Price     ║ Status   ║");
        System.out.println(  "╠════════════╬═════════╬═════════╬════════════╬══════════╬══════════╬══════════╬═════════════════╬══════════╣");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                String startTime = rs.getString("time");
                int dur = rs.getInt("duration");
                int startMins = timeToMinutes(startTime);
                int endMins = startMins + dur;
                String endTime = String.format("%02d:%02d", endMins / 60, endMins % 60);
                double totalPrice = rs.getDouble("totalPrice");

                System.out.printf("║ %-10d ║ %-7d ║ %-7d ║ %-10s ║ %-8s ║ %-8s ║ %-8d ║ PHP %-11.2f ║ %-8s ║%n",
                        rs.getInt("bookingId"),
                        rs.getInt("userId"),
                        rs.getInt("roomId"),
                        rs.getString("date"),
                        startTime,
                        endTime,
                        dur,
                        totalPrice,
                        truncate(rs.getString("status"), 8));
            }
            if (!found) {
                System.out.println("║                                         No bookings found.                                                  ║");
            }
        } catch (SQLException e) {
            System.out.println("Error printing bookings table: " + e.getMessage());
        }
        System.out.println("╚════════════╩═════════╩═════════╩════════════╩══════════╩══════════╩══════════╩═════════════════╩══════════╝");
    }

    // ─────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private int timeToMinutes(String time) {
        try {
            String[] parts = time.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }
}