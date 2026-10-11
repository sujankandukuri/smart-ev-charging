
package com.smartev.smart_ev_charging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
@RestController
public class SmartEvChargingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEvChargingApplication.class, args);
    }

    private final List<User> users = new ArrayList<>();
    private final List<Station> stations = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private int nextBookingId = 1;

    public SmartEvChargingApplication() {
        stations.add(new Station(1, "Central Charging Station", "Main Road", 10));
        stations.add(new Station(2, "City Mall Station", "City Mall", 8));
        stations.add(new Station(3, "Highway Charging Station", "National Highway", 12));
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "Running");
    }

    @PostMapping("/api/register")
    public Map<String, Object> register(@RequestBody Map<String, String> data) {
        String name = data.getOrDefault("name", "").trim();
        String email = data.getOrDefault("email", "").trim();

        if (name.isEmpty() || email.isEmpty()) {
            return Map.of("success", false, "message", "Enter your name and email.");
        }

        for (User user : users) {
            if (user.email.equalsIgnoreCase(email)) {
                return Map.of("success", false, "message", "This email is already registered.");
            }
        }

        User user = new User(users.size() + 1, name, email);
        users.add(user);

        return Map.of("success", true, "message", "Registration successful!", "user", user);
    }

    @GetMapping("/api/stations")
    public List<Station> getStations() {
        return stations;
    }

    @PostMapping("/api/stations/{id}/book")
    public Map<String, Object> bookStation(
            @PathVariable int id,
            @RequestBody(required = false) Map<String, String> data) {

        Station station = findStation(id);

        if (station == null) {
            return Map.of("success", false, "message", "Station not found.");
        }

        if (station.occupied >= station.capacity) {
            return Map.of("success", false, "message", "No available slots at this station.");
        }

        String email = data == null ? "" : data.getOrDefault("email", "").trim();

        if (email.isEmpty() || users.stream().noneMatch(
                user -> user.email.equalsIgnoreCase(email))) {
            return Map.of("success", false, "message", "Please register first and use your registered email.");
        }

        Booking booking = new Booking(
                nextBookingId++,
                station.id,
                station.name,
                email,
                "BOOKED",
                LocalDateTime.now().toString()
        );

        bookings.add(booking);
        station.occupied++;

        return Map.of("success", true, "message", "Charging slot booked successfully!", "booking", booking);
    }

    @PostMapping("/api/stations/{id}/complete")
    public Map<String, Object> completeBooking(@PathVariable int id) {
        Station station = findStation(id);

        if (station == null) {
            return Map.of("success", false, "message", "Station not found.");
        }

        for (int i = bookings.size() - 1; i >= 0; i--) {
            Booking booking = bookings.get(i);

            if (booking.stationId == id && booking.status.equals("BOOKED")) {
                booking.status = "COMPLETED";
                station.occupied--;

                return Map.of("success", true, "message", "Booking marked as completed.");
            }
        }

        return Map.of("success", false, "message", "No active booking found at this station.");
    }

    @GetMapping("/api/admin/summary")
    public Map<String, Object> adminSummary() {
        int totalSlots = stations.stream().mapToInt(s -> s.capacity).sum();
        int occupiedSlots = stations.stream().mapToInt(s -> s.occupied).sum();

        long activeBookings = bookings.stream()
                .filter(b -> b.status.equals("BOOKED")).count();

        long completedBookings = bookings.stream()
                .filter(b -> b.status.equals("COMPLETED")).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", users.size());
        result.put("totalStations", stations.size());
        result.put("totalBookings", bookings.size());
        result.put("activeBookings", activeBookings);
        result.put("completedBookings", completedBookings);
        result.put("totalSlots", totalSlots);
        result.put("occupiedSlots", occupiedSlots);
        result.put("availableSlots", totalSlots - occupiedSlots);

        return result;
    }

    @GetMapping("/api/admin/registrations")
    public List<User> adminRegistrations() {
        return users;
    }

    @GetMapping("/api/admin/stations")
    public List<Station> adminStations() {
        return stations;
    }

    @GetMapping("/api/admin/bookings")
    public List<Booking> adminBookings() {
        return bookings;
    }

    private Station findStation(int id) {
        return stations.stream().filter(s -> s.id == id).findFirst().orElse(null);
    }

    public static class User {
        public int id;
        public String name;
        public String email;

        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    public static class Station {
        public int id;
        public String name;
        public String location;
        public int capacity;
        public int occupied;

        public Station(int id, String name, String location, int capacity) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.capacity = capacity;
            this.occupied = 0;
        }

        public int getAvailableSlots() {
            return capacity - occupied;
        }
    }

    public static class Booking {
        public int id;
        public int stationId;
        public String stationName;
        public String userEmail;
        public String status;
        public String bookedAt;

        public Booking(int id, int stationId, String stationName,
                       String userEmail, String status, String bookedAt) {
            this.id = id;
            this.stationId = stationId;
            this.stationName = stationName;
            this.userEmail = userEmail;
            this.status = status;
            this.bookedAt = bookedAt;
        }
    }
}
