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

// Specific fields for Doctor
private TextField specField, expField;

// Specific fields for Patient
private DatePicker dobPicker;
private ComboBox<String> genderBox;

@FXML
public void handleRoleSelection() {
    dynamicContentContainer.getChildren().clear();
    String role = roleBox.getValue();
    if (role == null) return;

    String labelStyle = "-fx-text-fill: #555; -fx-font-size: 12px;";
    String fieldStyle = "-fx-background-radius: 6; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-color: white;";

    GridPane dynamicGrid = new GridPane();
    dynamicGrid.setHgap(20.0);
    dynamicGrid.setVgap(15.0);

    if ("Doctor".equals(role)) {
        // Specialization Box
        VBox specBox = new VBox(5.0);
        Label specLabel = new Label("Specialization");
        specLabel.setStyle(labelStyle);
        specField = new TextField();
        specField.setPromptText("Enter your specialization");
        specField.setStyle(fieldStyle);
        specBox.getChildren().addAll(specLabel, specField);

        // Experience Box
        VBox expBox = new VBox(5.0);
        Label expLabel = new Label("Years of Experience");
        expLabel.setStyle(labelStyle);
        expField = new TextField();
        expField.setPromptText("Enter experience");
        expField.setStyle(fieldStyle);
        expBox.getChildren().addAll(expLabel, expField);

        dynamicGrid.add(specBox, 0, 0);
        dynamicGrid.add(expBox, 1, 0);
        specBox.setPrefWidth(240);
        expBox.setPrefWidth(240);

    } else if ("Patient".equals(role)) {
        // DOB Box
        VBox dobBox = new VBox(5.0);
        Label dobLabel = new Label("Date of Birth");
        dobLabel.setStyle(labelStyle);
        dobPicker = new DatePicker();
        dobPicker.setPromptText("Select date");
        dobPicker.setMaxWidth(Double.MAX_VALUE);
        dobPicker.setStyle(fieldStyle);
        dobBox.getChildren().addAll(dobLabel, dobPicker);

        // Gender Box
        VBox genBox = new VBox(5.0);
        Label genLabel = new Label("Gender");
        genLabel.setStyle(labelStyle);
        genderBox = new ComboBox<>();
        genderBox.setPromptText("Select Gender");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        genderBox.getItems().addAll("Male", "Female", "Other");
        genderBox.setStyle(fieldStyle + " -fx-background-color: white;");
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

    String query = "INSERT INTO users (username, password, role, full_name, email, phone_number, specialization, experience_years, date_of_birth, gender) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pst = conn.prepareStatement(query)) {

        String role = roleBox.getValue();
        pst.setString(1, userField.getText());
        pst.setString(2, passField.getText());

        pst.setString(3, role);
        pst.setString(4, nameField.getText());
        pst.setString(5, emailField.getText());
        pst.setString(6, phoneField.getText());

        if ("Doctor".equals(role)) {
            pst.setString(7, specField.getText());
            pst.setInt(8, Integer.parseInt(expField.getText()));
            pst.setNull(9, java.sql.Types.DATE);
            pst.setNull(10, java.sql.Types.VARCHAR);
        } else {
            pst.setNull(7, java.sql.Types.VARCHAR);
            pst.setNull(8, java.sql.Types.INTEGER);
            pst.setDate(9, java.sql.Date.valueOf(dobPicker.getValue()));
            pst.setString(10, genderBox.getValue());
        }

        pst.executeUpdate();
        showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
        backToLogin();

    } catch (Exception e) {
        showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + e.getMessage());
        e.printStackTrace();
    }
}

private boolean validateInput() {
    // Check if required fields are empty
    if (roleBox.getValue() == null || userField.getText().isEmpty() || passField.getText().isEmpty() || nameField.getText().isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Missing Data", "Please fill all required fields.");
        return false;
    }

    // Check if passwords match
    if (!passField.getText().equals(confirmPassField.getText())) {
        showAlert(Alert.AlertType.WARNING, "Error", "Passwords do not match.");
        return false;
    }

    return true;
}

@FXML
public void backToLogin() {
    try {
        Stage stage = (Stage) userField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/caresync/ui/login.fxml"));
        stage.setScene(new Scene(root));
    } catch (Exception e) {
        e.printStackTrace();
    }
}

// Utility function to show alert messages
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}


}