package org.example;

public class Room {

    private final int roomId;
    private final String roomName;
    private final String roomType;
    private final int capacity;
    private final String availability;

    public Room(int roomId, String roomName, String roomType, int capacity, String availability) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.capacity = capacity;
        this.availability = availability;
    }

    // Getters
    public int getRoomId() {
        return roomId;
    }
    public String getRoomName() {
        return roomName;
    }
    public String getRoomType() {
        return roomType;
    }
    public int getCapacity() {
        return capacity;
    }
    public String getAvailability() {
        return availability;
    }

    // Optional display (NOT required but useful)
    public void display() {
        System.out.println("Room ID    : " + roomId);
        System.out.println("Room Name  : " + roomName);
        System.out.println("Type       : " + roomType);
        System.out.println("Capacity   : " + capacity);
        System.out.println("Status     : " + availability);
        System.out.println("---------------------------");
    }
}