package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalAppointments;
    @FXML private Label pendingAppointments;
    @FXML private Label approvedAppointments;
    @FXML private Label totalPatients;
    @FXML private Label totalDoctors;
    @FXML private TableView<AppointmentModel> appointmentsTable;
    @FXML private TableColumn<AppointmentModel, Integer> colId;
    @FXML private TableColumn<AppointmentModel, String> colPatient;
    @FXML private TableColumn<AppointmentModel, String> colDoctor;
    @FXML private TableColumn<AppointmentModel, String> colDate;
    @FXML private TableColumn<AppointmentModel, String> colTime;
    @FXML private TableColumn<AppointmentModel, String> colStatus;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + UserSession.getLoggedInUserName() + " (Admin)");
        loadDashboardStats();
        
        // Setup table columns if they exist in your FXML
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
            colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
            colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
    }

    public void loadDashboardStats() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            
            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) FROM appointments");
            if (rs1.next()) totalAppointments.setText(String.valueOf(rs1.getInt(1)));
            
            ResultSet rs2 = st.executeQuery("SELECT COUNT(*) FROM appointments WHERE status='Pending'");
            if (rs2.next()) pendingAppointments.setText(String.valueOf(rs2.getInt(1)));
            
            ResultSet rs3 = st.executeQuery("SELECT COUNT(*) FROM appointments WHERE status='Approved'");
            if (rs3.next()) approvedAppointments.setText(String.valueOf(rs3.getInt(1)));
            
            ResultSet rs4 = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='Patient'");
            if (rs4.next()) totalPatients.setText(String.valueOf(rs4.getInt(1)));
            
            ResultSet rs5 = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='Doctor'");
            if (rs5.next()) totalDoctors.setText(String.valueOf(rs5.getInt(1)));
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load statistics");
        }
    }
    
    private void loadAppointments() {
        ObservableList<AppointmentModel> list = FXCollections.observableArrayList();
        
        String sql = "SELECT a.appointmentId, " +
                     "p.full_name as patient_name, " +
                     "d.full_name as doctor_name, " +
                     "a.appointmentDate, a.appointmentTime, a.status " +
                     "FROM appointments a " +
                     "JOIN users p ON a.patientId = p.userId " +
                     "JOIN users d ON a.doctorId = d.userId " +
                     "ORDER BY a.appointmentDate DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Time sqlTime = rs.getTime("appointmentTime");
                String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                
                list.add(new AppointmentModel(
                    rs.getInt("appointmentId"),
                    rs.getString("patient_name"),
                    rs.getString("doctor_name"),
                    rs.getString("appointmentDate"),
                    timeStr,
                    rs.getString("status")
                ));
            }
            
            if (appointmentsTable != null) {
                appointmentsTable.setItems(list);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load appointments: " + e.getMessage());
        }
    }

    @FXML
    public void handleManageAppointments() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/caresync/ui/manage_appointments.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Manage Appointments");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            // If separate FXML doesn't exist, load appointments in current view
            if (appointmentsTable != null) {
                loadAppointments();
            } else {
                showAlert("Info", "Appointment management view coming soon!");
            }
        }
    }

    @FXML
    public void handleManageUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/caresync/ui/manage_users.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Manage Users");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showAlert("Info", "User management view coming soon!\n\nFeatures will include:\n• View all users\n• Add new users\n• Edit user details\n• Delete users");
        }
    }

    @FXML
    public void handleAppointmentRequest() {
        loadDashboardStats();
        
        // Also load pending appointments if table exists
        if (appointmentsTable != null) {
            ObservableList<AppointmentModel> pendingList = FXCollections.observableArrayList();
            
            String sql = "SELECT a.appointmentId, " +
                         "p.full_name as patient_name, " +
                         "d.full_name as doctor_name, " +
                         "a.appointmentDate, a.appointmentTime, a.status " +
                         "FROM appointments a " +
                         "JOIN users p ON a.patientId = p.userId " +
                         "JOIN users d ON a.doctorId = d.userId " +
                         "WHERE a.status = 'Pending' " +
                         "ORDER BY a.appointmentDate DESC";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                
                ResultSet rs = pst.executeQuery();
                
                while (rs.next()) {
                    Time sqlTime = rs.getTime("appointmentTime");
                    String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                    
                    pendingList.add(new AppointmentModel(
                        rs.getInt("appointmentId"),
                        rs.getString("patient_name"),
                        rs.getString("doctor_name"),
                        rs.getString("appointmentDate"),
                        timeStr,
                        rs.getString("status")
                    ));
                }
                
                appointmentsTable.setItems(pendingList);
                showAlert("Success", "Showing " + pendingList.size() + " pending appointment requests");
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load pending appointments");
            }
        } else {
            showAlert("Info", "Refreshed dashboard statistics");
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
    
    // Inner class for Appointment Model
    public static class AppointmentModel {
        private int appointmentId;
        private String patientName;
        private String doctorName;
        private String date;
        private String time;
        private String status;

        public AppointmentModel(int id, String patient, String doctor, String date, String time, String status) {
            this.appointmentId = id;
            this.patientName = patient;
            this.doctorName = doctor;
            this.date = date;
            this.time = time;
            this.status = status;
        }

        public int getAppointmentId() { return appointmentId; }
        public String getPatientName() { return patientName; }
        public String getDoctorName() { return doctorName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
    }
}