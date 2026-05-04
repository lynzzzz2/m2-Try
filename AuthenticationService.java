package org.example;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticationService {

    private final Repository repo;

    public AuthenticationService(Repository repo) {
        this.repo = repo;
    }

    // ─────────────────────────────────────────
    // EMAIL VALIDATION
    // ─────────────────────────────────────────

    public boolean isValidGmail(String email) {
        return email != null && email.toLowerCase().endsWith("@gmail.com") && email.length() > 10;
    }

    public String getEmailWarning(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email cannot be empty. Please put a proper Gmail (e.g. yourname@gmail.com).";
        }
        if (!email.contains("@")) {
            return "Please put a proper Gmail. You are missing '@gmail.com' (e.g. yourname@gmail.com).";
        }
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            return "Please put a proper Gmail. Only @gmail.com addresses are accepted (e.g. yourname@gmail.com).";
        }
        if (email.length() <= 10) {
            return "Please put a proper Gmail. Your email is too short (e.g. yourname@gmail.com).";
        }
        return null; // valid
    }

    // ─────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────

    public void registerUser(String name, String email, String password, String role) {
        if (!isValidGmail(email)) {
            System.out.println(getEmailWarning(email));
            return;
        }
        repo.insertUser(name, email, PasswordUtil.hash(password), role);
    }
    // ─────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────

    public int loginUser(String email, String password) {
        if (!isValidGmail(email)) {
            System.out.println(getEmailWarning(email));
            return -1;
        }

        ResultSet rs = repo.getUserByEmail(email);

        try {
            if (rs != null && rs.next()) {
                String storedPassword = rs.getString("password");
                String userName = rs.getString("name");

                if (PasswordUtil.verify(password, storedPassword)){
                    System.out.println("Login successful!");
                    System.out.println("Welcome, " + userName + " You can now Book your WorkingSpace <3");
                    return rs.getInt("userId");
                } else {
                    System.out.println("Incorrect password.");
                }
            } else {
                System.out.println("User not found.");
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.out.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        return -1;
    }

    public boolean adminPasswordExists() {
        return repo.getAdminPasswordHash() != null;
    }

    public void setAdminPassword(String newPassword) {
        repo.setAdminPasswordHash(PasswordUtil.hash(newPassword));
        System.out.println("Admin password set successfully.");
    }

    public boolean verifyAdminPassword(String input) {
        String stored = repo.getAdminPasswordHash();
        return stored != null && PasswordUtil.verify(input, stored);
    }
}
