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

public class DoctorDashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label todayAppointments;
    @FXML private Label pendingAppointments;
    @FXML private Label totalPatients;
    @FXML private TableView<?> appointmentsTable;
    @FXML private TableColumn<?, ?> colId, colPatient, colDate, colTime, colStatus, colDescription;
    
    @FXML
    public void initialize() {
        welcomeLabel.setText("Dr. " + UserSession.getLoggedInUserName());
        loadStats();
    }
    
    private void loadStats() {
        // Add your stats loading logic here
    }
    
    @FXML
    public void handleRefresh() {
        loadStats();
    }
    
    @FXML
    public void handleAssignedAppointment() {
        showAlert("Info", "Approve feature coming soon");
    }
    
    @FXML
    public void handleDailySchedules() {
        showAlert("Info", "Reject feature coming soon");
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
}