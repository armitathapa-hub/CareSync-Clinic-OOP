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

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
       
        if (roleBox != null) {
          
            if (roleBox.getItems().isEmpty()) {
                roleBox.getItems().addAll("Admin", "Doctor", "Patient");
            }
        }
    }

    @FXML
    public void loginUser() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleBox.getValue();

        if (user.isEmpty() || pass.isEmpty() || role == null) {
            updateStatus("Please fill all fields!", "#e74c3c");
            return;
        }

        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, user);
            pst.setString(2, pass);
            pst.setString(3, role);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                updateStatus("Login Successful!", "#0359A4");
                caresync.database.UserSession.setLoggedInUserName(rs.getString("full_name"));
                caresync.database.UserSession.setLoggedInUserId(rs.getInt("userId"));
                caresync.database.UserSession.setLoggedInRole(role);

             
                try {
                    String fxmlFile = "";
                    if (role.equals("Admin")) {
                        fxmlFile = "/caresync/ui/admindashboard.fxml";
                    } else if (role.equals("Doctor")) {
                        fxmlFile = "/caresync/ui/doctordashboard.fxml";
                    } else if (role.equals("Patient")) {
                        fxmlFile = "/caresync/ui/patientdashboard.fxml";
                    }

                    System.out.println("Loading: " + fxmlFile);
                    
                  
                    if (getClass().getResource(fxmlFile) == null) {
                        updateStatus("ERROR: Cannot find " + fxmlFile, "#e74c3c");
                        System.err.println("FXML file not found: " + fxmlFile);
                        return;
                    }

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
                    stage.setScene(new Scene(root));
                    stage.setTitle("CareSync - " + role + " Dashboard");
                    stage.show();

                } catch (Exception e) {
                    updateStatus("Dashboard Error: " + e.getMessage(), "#e74c3c");
                    e.printStackTrace();
                }

            } else {
                updateStatus("Invalid credentials!", "#e74c3c");
            }
        } catch (SQLException e) {
            updateStatus("Database Error: " + e.getMessage(), "#e74c3c");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            // Check if register.fxml exists
            if (getClass().getResource("/caresync/ui/register.fxml") == null) {
                updateStatus("Register FXML not found!", "#e74c3c");
                return;
            }
            
            Parent root = FXMLLoader.load(getClass().getResource("/caresync/ui/register.fxml"));
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            
        } catch (Exception e) {
            updateStatus("Error loading register page: " + e.getMessage(), "#e74c3c");
            e.printStackTrace();
        }
    }

    private void updateStatus(String message, String color) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        } else {
            System.out.println("Status: " + message + " (Color: " + color + ")");
        }
    }
}