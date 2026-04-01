package caresync.controller;

import caresync.database.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class RegisterController {

    @FXML private TextField nameField, emailField, phoneField, userField;
    @FXML private PasswordField passField, confirmPassField;
    @FXML private ComboBox<String> roleBox;
    @FXML private VBox dynamicContentContainer;

    // Doctor-specific fields
    private TextField specField, expField;

    // Patient-specific fields
    private DatePicker dobPicker;
    private ComboBox<String> genderBox;

    @FXML
    public void handleRoleSelection() {
        dynamicContentContainer.getChildren().clear();
        String role = roleBox.getValue();
        if (role == null) return;

        String labelStyle = "-fx-text-fill: #444; -fx-font-size: 12px; -fx-font-weight: bold;";
        String fieldStyle = "-fx-background-radius: 8; -fx-padding: 10; " +
                            "-fx-border-color: #dde4ee; -fx-border-radius: 8; " +
                            "-fx-background-color: #fafdff; -fx-font-size: 13px;";

        GridPane dynamicGrid = new GridPane();
        dynamicGrid.setHgap(18.0);
        dynamicGrid.setVgap(12.0);

        if ("Doctor".equals(role)) {
            VBox specBox = new VBox(5.0);
            Label specLabel = new Label("Specialization *");
            specLabel.setStyle(labelStyle);
            specField = new TextField();
            specField.setPromptText("e.g. Cardiologist, Dentist");
            specField.setStyle(fieldStyle);
            specBox.getChildren().addAll(specLabel, specField);

            VBox expBox = new VBox(5.0);
            Label expLabel = new Label("Years of Experience *");
            expLabel.setStyle(labelStyle);
            expField = new TextField();
            expField.setPromptText("e.g. 5");
            expField.setStyle(fieldStyle);
            expBox.getChildren().addAll(expLabel, expField);

            dynamicGrid.add(specBox, 0, 0);
            dynamicGrid.add(expBox, 1, 0);
            specBox.setPrefWidth(240);
            expBox.setPrefWidth(240);

        } else if ("Patient".equals(role)) {
            VBox dobBox = new VBox(5.0);
            Label dobLabel = new Label("Date of Birth *");
            dobLabel.setStyle(labelStyle);
            dobPicker = new DatePicker();
            dobPicker.setPromptText("Select your date of birth");
            dobPicker.setMaxWidth(Double.MAX_VALUE);
            dobPicker.setStyle(fieldStyle);
            dobBox.getChildren().addAll(dobLabel, dobPicker);

            VBox genBox = new VBox(5.0);
            Label genLabel = new Label("Gender *");
            genLabel.setStyle(labelStyle);
            genderBox = new ComboBox<>();
            genderBox.setPromptText("Select Gender");
            genderBox.setMaxWidth(Double.MAX_VALUE);
            genderBox.getItems().addAll("Male", "Female", "Other");
            genderBox.setStyle(fieldStyle + " -fx-background-color: #fafdff;");
            genBox.getChildren().addAll(genLabel, genderBox);

            dynamicGrid.add(dobBox, 0, 0);
            dynamicGrid.add(genBox, 1, 0);
            dobBox.setPrefWidth(240);
            genBox.setPrefWidth(240);
        }

        dynamicContentContainer.getChildren().add(dynamicGrid);
    }

    @FXML
    public void registerUser() {
        if (!validateInput()) return;

        String query = "INSERT INTO users (username, password, role, full_name, email, phone_number, " +
                       "specialization, experience_years, date_of_birth, gender) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            String role = roleBox.getValue();
            pst.setString(1, userField.getText().trim());
            pst.setString(2, passField.getText());
            pst.setString(3, role);
            pst.setString(4, nameField.getText().trim());
            pst.setString(5, emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
            pst.setString(6, phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

            if ("Doctor".equals(role)) {
                pst.setString(7, specField.getText().trim().isEmpty() ? null : specField.getText().trim());
                String expText = expField.getText().trim();
                if (!expText.isEmpty()) {
                    pst.setInt(8, Integer.parseInt(expText));
                } else {
                    pst.setNull(8, java.sql.Types.INTEGER);
                }
                pst.setNull(9, java.sql.Types.DATE);
                pst.setNull(10, java.sql.Types.VARCHAR);
            } else {
                pst.setNull(7, java.sql.Types.VARCHAR);
                pst.setNull(8, java.sql.Types.INTEGER);
                if (dobPicker.getValue() != null) {
                    pst.setDate(9, java.sql.Date.valueOf(dobPicker.getValue()));
                } else {
                    pst.setNull(9, java.sql.Types.DATE);
                }
                pst.setString(10, genderBox.getValue());
            }

            pst.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                    "Your account has been created!\nYou can now log in with your credentials.");
            backToLogin();

        } catch (SQLIntegrityConstraintViolationException e) {
            showAlert(Alert.AlertType.ERROR, "Username Taken",
                    "The username '" + userField.getText().trim() + "' is already taken.\nPlease choose a different username.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Registration Failed", "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        String role = roleBox.getValue();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String username = userField.getText().trim();
        String password = passField.getText();
        String confirmPassword = confirmPassField.getText();

        // Required: role
        if (role == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Please select a role (Patient or Doctor).");
            return false;
        }

        // Required: full name
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Full Name is required.");
            return false;
        }

        // Required: username
        if (username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Username is required.");
            return false;
        }
        if (username.length() < 4) {
            showAlert(Alert.AlertType.WARNING, "Invalid Username", "Username must be at least 4 characters long.");
            return false;
        }

        // Required: password
        if (password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Password is required.");
            return false;
        }
        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password", "Password must be at least 6 characters long.");
            return false;
        }

        // Passwords must match
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Password Mismatch", "Passwords do not match. Please re-enter.");
            return false;
        }

        // Email format validation (optional but if provided must be valid)
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address.\nExample: name@email.com");
            return false;
        }

        // Required: phone number
        if (phone.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Phone Number is required.");
            return false;
        }

        // Phone number format validation (10-15 digits, no spaces or symbols)
        if (!phone.matches("^[0-9]{10,15}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Phone Number",
                    "Phone number must be 10–15 digits with no spaces or symbols.");
            return false;
        }

        // Phone number duplication check
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE phone_number = ?")) {
            pst.setString(1, phone);
            ResultSet rs = pst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.WARNING, "Phone Number Taken",
                        "The phone number '" + phone + "' is already registered.\nPlease use a different phone number.");
                return false;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not verify phone number: " + e.getMessage());
            return false;
        }

        // Doctor-specific validations
        if ("Doctor".equals(role)) {
            if (specField == null || specField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Field", "Specialization is required for Doctors.");
                return false;
            }
            if (expField == null || expField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Field", "Years of Experience is required for Doctors.");
                return false;
            }
            try {
                int exp = Integer.parseInt(expField.getText().trim());
                if (exp < 0 || exp > 60) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Experience",
                            "Years of experience must be between 0 and 60.");
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Experience",
                        "Years of experience must be a number (e.g. 5).");
                return false;
            }
        }

        // Patient-specific validations
        if ("Patient".equals(role)) {
            if (dobPicker == null || dobPicker.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Missing Field", "Date of Birth is required for Patients.");
                return false;
            }
            if (dobPicker.getValue().isAfter(java.time.LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Invalid Date of Birth",
                        "Date of Birth cannot be in the future.");
                return false;
            }
            if (genderBox == null || genderBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Missing Field", "Please select a gender.");
                return false;
            }
        }

        return true;
    }

    @FXML
    public void backToLogin() {
        try {
            Stage stage = (Stage) userField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/caresync/ui/login.fxml"));
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}