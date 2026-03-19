package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class BookingModalController {

    @FXML private ComboBox<String> doctorSelect;
    @FXML private DatePicker dateSelect;
    @FXML private TextField timeSelect;
    @FXML private TextArea healthNote;

    @FXML
    public void initialize() {
        System.out.println("Booking modal initialized");
        loadDoctorsFromDatabase();
    }

    private void loadDoctorsFromDatabase() {
        String sql = "SELECT full_name FROM users WHERE role = 'Doctor'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            doctorSelect.getItems().clear();
            while (rs.next()) {
                doctorSelect.getItems().add(rs.getString("full_name"));
            }
            System.out.println("Loaded " + doctorSelect.getItems().size() + " doctors");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load doctors: " + e.getMessage());
        }
    }

    @FXML
    public void handleBookingSubmit() {
        String doctor = doctorSelect.getValue();
        String date = (dateSelect.getValue() != null) ? dateSelect.getValue().toString() : "";
        String time = timeSelect.getText();
        String note = healthNote.getText();

        if (doctor == null || date.isEmpty() || time.isEmpty()) {
            showAlert("Warning", "Please fill all required fields!");
            return;
        }

        // Validate time format (HH:MM)
        if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            showAlert("Error", "Please enter time in HH:MM format (e.g., 14:30)");
            return;
        }

        String sql = "INSERT INTO appointments (patientId, doctorId, appointmentDate, appointmentTime, status, description) " +
                     "VALUES ((SELECT userId FROM users WHERE full_name = ?), " +
                     "(SELECT userId FROM users WHERE full_name = ?), ?, ?, 'Pending', ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, UserSession.getLoggedInUserName());
            pst.setString(2, doctor);
            pst.setString(3, date);
            pst.setString(4, time);
            pst.setString(5, note);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                showAlert("Success", "Appointment booked successfully!");

                // Close the popup window
                Stage stage = (Stage) healthNote.getScene().getWindow();
                stage.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to book appointment: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}