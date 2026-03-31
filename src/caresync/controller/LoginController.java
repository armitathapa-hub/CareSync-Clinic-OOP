package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import caresync.model.Person;
import caresync.model.AdminUser;
import caresync.model.DoctorUser;
import caresync.model.PatientUser;
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
                
                // Get user data
                int userId = rs.getInt("userId");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email") != null ? rs.getString("email") : "";
                String specialization = rs.getString("specialization");
                int experienceYears = rs.getInt("experience_years");
                String dateOfBirth = rs.getString("date_of_birth");
                String gender = rs.getString("gender");
                
                // Set session data
                caresync.database.UserSession.setLoggedInUserName(fullName);
                caresync.database.UserSession.setLoggedInUserId(userId);
                caresync.database.UserSession.setLoggedInRole(role);
                
                // ========== OOP DEMONSTRATION: INHERITANCE & POLYMORPHISM ==========
                // Creating Person objects based on role - This shows inheritance and polymorphism
                Person loggedInPerson = null;
                
                if (role.equals("Admin")) {
                    loggedInPerson = new AdminUser(userId, fullName, email);
                    System.out.println("=== OOP DEMONSTRATION ===");
                    System.out.println("Created AdminUser object using inheritance from Person");
                    System.out.println("Role: " + loggedInPerson.getRole());
                    System.out.println("Dashboard: " + loggedInPerson.getDashboardView());
                } else if (role.equals("Doctor")) {
                    loggedInPerson = new DoctorUser(userId, fullName, email, specialization, experienceYears);
                    System.out.println("=== OOP DEMONSTRATION ===");
                    System.out.println("Created DoctorUser object using inheritance from Person");
                    System.out.println("Role: " + loggedInPerson.getRole());
                    System.out.println("Dashboard: " + loggedInPerson.getDashboardView());
                    // Cast to access doctor-specific methods
                    if (loggedInPerson instanceof DoctorUser) {
                        DoctorUser doctor = (DoctorUser) loggedInPerson;
                        System.out.println("Specialization: " + doctor.getSpecialization());
                        System.out.println("Experience: " + doctor.getExperienceYears() + " years");
                    }
                } else if (role.equals("Patient")) {
                    loggedInPerson = new PatientUser(userId, fullName, email, dateOfBirth, gender);
                    System.out.println("=== OOP DEMONSTRATION ===");
                    System.out.println("Created PatientUser object using inheritance from Person");
                    System.out.println("Role: " + loggedInPerson.getRole());
                    System.out.println("Dashboard: " + loggedInPerson.getDashboardView());
                    // Cast to access patient-specific methods
                    if (loggedInPerson instanceof PatientUser) {
                        PatientUser patient = (PatientUser) loggedInPerson;
                        System.out.println("Date of Birth: " + patient.getDateOfBirth());
                        System.out.println("Gender: " + patient.getGender());
                    }
                }
                // ========== END OOP DEMONSTRATION ==========

                // Load dashboard
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