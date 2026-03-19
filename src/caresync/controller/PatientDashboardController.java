package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.*;

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AppointmentModel> appointmentTable;
    @FXML private TableColumn<AppointmentModel, Integer> colId;
    @FXML private TableColumn<AppointmentModel, String> colDoctor;
    @FXML private TableColumn<AppointmentModel, String> colDate;
    @FXML private TableColumn<AppointmentModel, String> colTime;
    @FXML private TableColumn<AppointmentModel, String> colStatus;
    
    // Sidebar buttons
    @FXML private Button bookAppointmentBtn;
    @FXML private Button viewStatusBtn;

    @FXML
    public void initialize() {
        // Set welcome message
        welcomeLabel.setText("Welcome " + UserSession.getLoggedInUserName() + "!");

        // Link columns
        colId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Force table to remove extra columns
        appointmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Load data
        loadAppointments();
        
        // Setup sidebar button actions if they're defined in FXML
        if (bookAppointmentBtn != null) {
            bookAppointmentBtn.setOnAction(e -> openBookingModal());
        }
        
        if (viewStatusBtn != null) {
            viewStatusBtn.setOnAction(e -> viewAppointmentStatus());
        }
    }

    private void loadAppointments() {
        ObservableList<AppointmentModel> list = FXCollections.observableArrayList();

        String sql = "SELECT a.appointmentId, u.full_name as doctor_name, " +
                     "a.appointmentDate, a.appointmentTime, a.status " +
                     "FROM appointments a JOIN users u ON a.doctorId = u.userId " +
                     "WHERE a.patientId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, UserSession.getLoggedInUserId());
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Time sqlTime = rs.getTime("appointmentTime");
                String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";

                list.add(new AppointmentModel(
                    rs.getInt("appointmentId"),
                    rs.getString("doctor_name"),
                    rs.getString("appointmentDate"),
                    timeStr,
                    rs.getString("status")
                ));
            }
            appointmentTable.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load appointments: " + e.getMessage());
        }
    }
    
    @FXML
    public void viewAppointmentStatus() {
        // This is triggered by the "View Status" sidebar button
        loadAppointments(); // Refresh the table
        showAlert("Appointment Status", "Showing all your appointments with their current status.\n\n" +
                  "Status meanings:\n" +
                  "• Pending: Waiting for doctor approval\n" +
                  "• Approved: Appointment confirmed\n" +
                  "• Completed: Appointment finished\n" +
                  "• Cancelled: Appointment cancelled");
    }

    @FXML
    public void openBookingModal() {
        try {
            System.out.println("Attempting to open booking modal...");

            // Check multiple possible paths
            String[] possiblePaths = {
                "/caresync/ui/booking_modal.fxml",
                "/caresync/ui/bookingmodal.fxml",
                "/caresync/ui/BookingModal.fxml",
                "caresync/ui/booking_modal.fxml"
            };

            FXMLLoader loader = null;
            Parent root = null;

            for (String path : possiblePaths) {
                System.out.println("Trying path: " + path);
                if (getClass().getResource(path) != null) {
                    System.out.println("Found at: " + path);
                    loader = new FXMLLoader(getClass().getResource(path));
                    root = loader.load();
                    break;
                }
            }

            if (root == null) {
                showAlert("Error", "Could not find booking_modal.fxml!\nMake sure it exists in: src/caresync/ui/");
                return;
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Book New Appointment");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh table after booking
            loadAppointments();

        } catch (Exception e) {
            System.err.println("Error opening booking modal:");
            e.printStackTrace();
            showAlert("Error", "Could not open booking form: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            UserSession.clearSession();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/caresync/ui/login.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AppointmentModel {
        private int appointmentId;
        private String doctorName;
        private String date;
        private String time;
        private String status;

        public AppointmentModel(int id, String doc, String date, String time, String status) {
            this.appointmentId = id;
            this.doctorName = doc;
            this.date = date;
            this.time = time;
            this.status = status;
        }

        public int getAppointmentId() { return appointmentId; }
        public String getDoctorName() { return doctorName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
    }
}