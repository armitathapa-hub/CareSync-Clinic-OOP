package caresync.model;

public class Appointment {
    private int id;
    private String doctor;
    private String date;
    private String time;
    private String status;

    public Appointment(int id, String doctor, String date, String time, String status) {
        this.id = id;           // Fixed: was [this.id] which is incorrect
        this.doctor = doctor;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public String getDoctor() { return doctor; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
}