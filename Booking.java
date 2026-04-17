package org.example;

public class Booking {

    private final int bookingId;
    private final int userId;
    private final int roomId;
    private final String date;
    private final String time;
    private final int duration;
    private final String status;

    public Booking(int bookingId, int userId, int roomId, String date, String time, int duration, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.roomId = roomId;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.status = status;
    }

    public int getBookingId()  { return bookingId; }
    public int getUserId()     { return userId; }
    public int getRoomId()     { return roomId; }
    public String getDate()    { return date; }
    public String getTime()    { return time; }
    public int getDuration()   { return duration; }
    public String getStatus()  { return status; }

    public void display() {
        int endMins = timeToMinutes(time) + duration;
        String endTime = String.format("%02d:%02d", endMins / 60, endMins % 60);

        System.out.println("Booking ID : " + bookingId);
        System.out.println("User ID    : " + userId);
        System.out.println("Room ID    : " + roomId);
        System.out.println("Date       : " + date);
        System.out.println("Start Time : " + time);
        System.out.println("End Time   : " + endTime);
        System.out.println("Duration   : " + duration + " minutes");
        System.out.println("Status     : " + status);
        System.out.println("---------------------------");
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