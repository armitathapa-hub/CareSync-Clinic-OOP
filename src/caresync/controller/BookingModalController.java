package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class BookingModalController {

    @FXML private ComboBox<String> doctorSelect;
    @FXML private DatePicker dateSelect;
    @FXML private TextField timeSelect;
    @FXML private TextArea healthNote;

    @FXML
    public void initialize() {
        loadDoctorsFromDatabase();

        // Disable past dates in the date picker
        dateSelect.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date.isBefore(LocalDate.now())) {
                    setStyle("-fx-background-color: #f8d7da;");
                }
            }
        });

        // Real-time time field validation with visual feedback
        timeSelect.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                timeSelect.setStyle("-fx-background-radius: 8; -fx-border-color: #d0dce8; " +
                                    "-fx-border-radius: 8; -fx-background-color: #fafdff; -fx-font-size: 13px; -fx-padding: 10;");
            } else if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                timeSelect.setStyle("-fx-background-radius: 8; -fx-border-color: #e74c3c; " +
                                    "-fx-border-radius: 8; -fx-background-color: #fff5f5; -fx-font-size: 13px; -fx-padding: 10;");
            } else {
                timeSelect.setStyle("-fx-background-radius: 8; -fx-border-color: #27ae60; " +
                                    "-fx-border-radius: 8; -fx-background-color: #f0fff4; -fx-font-size: 13px; -fx-padding: 10;");
            }
        });
    }

    private void loadDoctorsFromDatabase() {
        String sql = "SELECT full_name, specialization FROM users WHERE role = 'Doctor' ORDER BY full_name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            doctorSelect.getItems().clear();
            while (rs.next()) {
                String doctorName = rs.getString("full_name");
                String specialization = rs.getString("specialization");

                if (specialization != null && !specialization.isEmpty()) {
                    doctorSelect.getItems().add(doctorName + " (" + specialization + ")");
                } else {
                    doctorSelect.getItems().add(doctorName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load doctors: " + e.getMessage());
        }
    }

    private String extractDoctorName(String displayName) {
        if (displayName != null && displayName.contains(" (")) {
            return displayName.substring(0, displayName.indexOf(" (")).trim();
        }
        return displayName;
    }

    private boolean isPastDateTime(LocalDate date, String timeStr) {
        if (date == null || timeStr == null || timeStr.isEmpty()) return true;
        try {
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime appointmentDateTime = LocalDateTime.of(date, time);
            return appointmentDateTime.isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isWithinWorkingHours(String timeStr) {
        try {
            LocalTime time = LocalTime.parse(timeStr);
            LocalTime startTime = LocalTime.of(9, 0);
            LocalTime endTime = LocalTime.of(17, 0);
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    public void handleBookingSubmit() {
        String doctorDisplay = doctorSelect.getValue();
        String doctorName = extractDoctorName(doctorDisplay);
        LocalDate date = dateSelect.getValue();
        String time = timeSelect.getText().trim();
        String note = healthNote.getText().trim();

        // Validation 1: Required fields
        if (doctorDisplay == null || doctorDisplay.isEmpty()) {
            showAlert("Missing Field", "Please select a doctor.");
            return;
        }
        if (date == null) {
            showAlert("Missing Field", "Please select an appointment date.");
            return;
        }
        if (time.isEmpty()) {
            showAlert("Missing Field", "Please enter an appointment time.");
            return;
        }

        // Validation 2: Time format
        if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            showAlert("Invalid Time Format",
                    "Please enter time in HH:MM format.\n\nExamples: 09:00, 14:30, 16:45");
            return;
        }

        // Validation 3: Past date/time
        if (isPastDateTime(date, time)) {
            showAlert("Invalid Date/Time",
                    "You cannot book an appointment in the past.\n\nPlease choose a future date and time.");
            return;
        }

        // Validation 4: Working hours
        if (!isWithinWorkingHours(time)) {
            showAlert("Outside Working Hours",
                    "Appointments are only available between 9:00 AM and 5:00 PM.\n\nPlease select a valid time.");
            return;
        }

        // Validation 5: Duplicate appointment (same patient, doctor, date, time)
        if (isDuplicateAppointment(doctorName, date.toString(), time)) {
            showAlert("Duplicate Appointment",
                    "You already have an appointment with this doctor at the same date and time.\n\nPlease choose a different slot.");
            return;
        }

        // Validation 6: Doctor already booked at that time
        if (isDoctorBusy(doctorName, date.toString(), time)) {
            showAlert("Doctor Unavailable",
                    "This doctor is already booked at " + time + " on " + date + ".\n\nPlease choose a different time.");
            return;
        }

        // Insert appointment
        String sql = "INSERT INTO appointments (patientId, doctorId, appointmentDate, appointmentTime, status, description) " +
                     "VALUES (" +
                     "(SELECT userId FROM users WHERE userId = ?), " +
                     "(SELECT userId FROM users WHERE full_name = ? AND role = 'Doctor'), " +
                     "?, ?, 'Pending', ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, UserSession.getLoggedInUserId());
            pst.setString(2, doctorName);
            pst.setString(3, date.toString());
            pst.setString(4, time);
            pst.setString(5, note.isEmpty() ? null : note);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                showAlert("Booking Successful ✔",
                        "Your appointment has been booked!\n\n" +
                        "Doctor: " + doctorDisplay + "\n" +
                        "Date:   " + date + "\n" +
                        "Time:   " + time + "\n\n" +
                        "Status: Pending (awaiting admin approval)");

                Stage stage = (Stage) healthNote.getScene().getWindow();
                stage.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to book appointment:\n" + e.getMessage());
        }
    }

    // Cancel button handler — fixes the original bug where CANCEL called handleBookingSubmit
    @FXML
    public void handleCancel() {
        Stage stage = (Stage) healthNote.getScene().getWindow();
        stage.close();
    }

    private boolean isDoctorBusy(String doctor, String date, String time) {
        String sql = "SELECT COUNT(*) FROM appointments a " +
                    "JOIN users u ON a.doctorId = u.userId " +
                    "WHERE u.full_name = ? AND u.role = 'Doctor' " +
                    "AND a.appointmentDate = ? AND a.appointmentTime = ? " +
                    "AND a.status NOT IN ('Cancelled', 'Rejected')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, doctor);
            pst.setString(2, date);
            pst.setString(3, time);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isDuplicateAppointment(String doctor, String date, String time) {
        String sql = "SELECT COUNT(*) FROM appointments a " +
                    "WHERE a.patientId = ? " +
                    "AND a.doctorId = (SELECT userId FROM users WHERE full_name = ? AND role = 'Doctor') " +
                    "AND a.appointmentDate = ? AND a.appointmentTime = ? " +
                    "AND a.status NOT IN ('Cancelled', 'Completed', 'Rejected')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, UserSession.getLoggedInUserId());
            pst.setString(2, doctor);
            pst.setString(3, date);
            pst.setString(4, time);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}