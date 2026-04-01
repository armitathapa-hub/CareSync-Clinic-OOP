package caresync.controller;

import caresync.database.DBConnection;
import caresync.database.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Optional;
import java.sql.Types;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalAppointments;
    @FXML private Label pendingAppointments;
    @FXML private Label approvedAppointments;
    @FXML private Label totalPatients;
    @FXML private Label totalDoctors;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + UserSession.getLoggedInUserName() + " (Admin)");
        loadDashboardStats();
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

    @FXML
    public void handleManageAppointments() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Manage Appointments");
            stage.initModality(Modality.APPLICATION_MODAL);
            
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: white;");
            
            Label title = new Label("All Appointments");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            
            TableView<AppointmentData> table = new TableView<>();
            
            TableColumn<AppointmentData, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setPrefWidth(50);
            
            TableColumn<AppointmentData, String> patientCol = new TableColumn<>("Patient");
            patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
            patientCol.setPrefWidth(180);
            
            TableColumn<AppointmentData, String> doctorCol = new TableColumn<>("Doctor");
            doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
            doctorCol.setPrefWidth(180);
            
            TableColumn<AppointmentData, String> specializationCol = new TableColumn<>("Specialization");
            specializationCol.setCellValueFactory(new PropertyValueFactory<>("specialization"));
            specializationCol.setPrefWidth(150);
            
            TableColumn<AppointmentData, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            dateCol.setPrefWidth(100);
            
            TableColumn<AppointmentData, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
            timeCol.setPrefWidth(80);
            
            TableColumn<AppointmentData, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setPrefWidth(100);
            
            TableColumn<AppointmentData, Void> actionCol = new TableColumn<>("Action");
            actionCol.setPrefWidth(120);
            
            actionCol.setCellFactory(param -> new TableCell<AppointmentData, Void>() {
                private final Button actionBtn = new Button();
                private final HBox buttons = new HBox(5);
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        AppointmentData appointment = getTableView().getItems().get(getIndex());
                        String status = appointment.getStatus();
                        buttons.getChildren().clear();
                        
                        if (status.equals("Pending")) {
                            Button approveBtn = new Button("Approve");
                            approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10;");
                            approveBtn.setOnAction(e -> updateAppointmentStatus(appointment.getId(), "Approved", table));
                            buttons.getChildren().add(approveBtn);
                        } else if (status.equals("Approved")) {
                        } else {
                            Label statusLabel = new Label(status);
                            statusLabel.setStyle("-fx-text-fill: #95a5a6;");
                            buttons.getChildren().add(statusLabel);
                        }
                        setGraphic(buttons);
                    }
                }
            });
            
            table.getColumns().addAll(idCol, patientCol, doctorCol, specializationCol, dateCol, timeCol, statusCol, actionCol);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            ObservableList<AppointmentData> appointments = FXCollections.observableArrayList();
            
            String sql = "SELECT a.appointmentId, p.full_name as patientName, d.full_name as doctorName, " +
                        "d.specialization, a.appointmentDate, a.appointmentTime, a.status " +
                        "FROM appointments a " +
                        "JOIN users p ON a.patientId = p.userId " +
                        "JOIN users d ON a.doctorId = d.userId " +
                        "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC";
            
            try (Connection conn = DBConnection.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                
                while (rs.next()) {
                    Time sqlTime = rs.getTime("appointmentTime");
                    String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                    String specialization = rs.getString("specialization");
                    if (specialization == null || specialization.isEmpty()) {
                        specialization = "General";
                    }
                    
                    appointments.add(new AppointmentData(
                        rs.getInt("appointmentId"),
                        rs.getString("patientName"),
                        rs.getString("doctorName"),
                        specialization,
                        rs.getString("appointmentDate"),
                        timeStr,
                        rs.getString("status")
                    ));
                }
            }
            
            table.setItems(appointments);
            vbox.getChildren().addAll(title, table);
            
            Label countLabel = new Label("Total: " + appointments.size() + " appointments");
            countLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 10 0 0 0;");
            vbox.getChildren().add(countLabel);
            
            Scene scene = new Scene(vbox, 1100, 600);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load appointments: " + e.getMessage());
        }
    }
    
    private void updateAppointmentStatus(int appointmentId, String newStatus, TableView<AppointmentData> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm " + newStatus);
        confirm.setHeaderText(newStatus + " Appointment");
        confirm.setContentText("Are you sure you want to mark this appointment as " + newStatus + "?");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            String sql = "UPDATE appointments SET status = ? WHERE appointmentId = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, newStatus);
                pst.setInt(2, appointmentId);
                pst.executeUpdate();
                
                showAlert("Success", "Appointment " + newStatus.toLowerCase() + " successfully!");
                loadDashboardStats();
                
                table.getItems().clear();
                String sql2 = "SELECT a.appointmentId, p.full_name as patientName, d.full_name as doctorName, " +
                            "d.specialization, a.appointmentDate, a.appointmentTime, a.status " +
                            "FROM appointments a " +
                            "JOIN users p ON a.patientId = p.userId " +
                            "JOIN users d ON a.doctorId = d.userId " +
                            "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC";
                
                try (Connection conn2 = DBConnection.getConnection();
                     Statement st = conn2.createStatement();
                     ResultSet rs = st.executeQuery(sql2)) {
                    
                    while (rs.next()) {
                        Time sqlTime = rs.getTime("appointmentTime");
                        String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                        String specialization = rs.getString("specialization");
                        if (specialization == null || specialization.isEmpty()) {
                            specialization = "General";
                        }
                        
                        table.getItems().add(new AppointmentData(
                            rs.getInt("appointmentId"),
                            rs.getString("patientName"),
                            rs.getString("doctorName"),
                            specialization,
                            rs.getString("appointmentDate"),
                            timeStr,
                            rs.getString("status")
                        ));
                    }
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Could not update appointment: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleManageUsers() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Manage Users");
            stage.initModality(Modality.APPLICATION_MODAL);
            
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            
            Tab patientsTab = new Tab("Patients");
            VBox patientsContent = createUserTable("Patient", stage);
            patientsTab.setContent(patientsContent);
            
            Tab doctorsTab = new Tab("Doctors");
            VBox doctorsContent = createUserTable("Doctor", stage);
            doctorsTab.setContent(doctorsContent);
            
            Tab adminsTab = new Tab("Admins");
            VBox adminsContent = createUserTable("Admin", stage);
            adminsTab.setContent(adminsContent);
            
            tabPane.getTabs().addAll(patientsTab, doctorsTab, adminsTab);
            
            Scene scene = new Scene(tabPane, 1000, 600);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load users: " + e.getMessage());
        }
    }
    
    private VBox createUserTable(String role, Stage parentStage) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        
        Button addNewBtn = null;
        if (!role.equals("Admin")) {
            addNewBtn = new Button("+ Add New " + role);
            addNewBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold;");
            addNewBtn.setOnAction(e -> showAddUserDialog(role, parentStage));
        }
        
        TableView<UserData> table = new TableView<>();
        
        TableColumn<UserData, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<UserData, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(200);
        
        TableColumn<UserData, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);
        
        TableColumn<UserData, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(200);
        
        TableColumn<UserData, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(120);
        
        TableColumn<UserData, String> specCol = new TableColumn<>("Specialization");
        specCol.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        specCol.setPrefWidth(150);
        
        TableColumn<UserData, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("actionButton"));
        actionCol.setPrefWidth(100);
        
        if (role.equals("Doctor")) {
            table.getColumns().addAll(idCol, nameCol, usernameCol, emailCol, phoneCol, specCol, actionCol);
        } else {
            table.getColumns().addAll(idCol, nameCol, usernameCol, emailCol, phoneCol, actionCol);
        }
        
        ObservableList<UserData> users = FXCollections.observableArrayList();
        
        String sql = "SELECT userId, full_name, username, email, phone_number, specialization FROM users WHERE role = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, role);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
                int userId = rs.getInt("userId");
                String userName = rs.getString("full_name");
                
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Delete");
                    confirm.setHeaderText("Delete " + role);
                    confirm.setContentText("Are you sure you want to delete " + userName + "?");
                    
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        deleteUser(userId);
                        users.removeIf(user -> user.getId() == userId);
                        loadDashboardStats();
                    }
                });
                
                UserData userData = new UserData(
                    userId,
                    rs.getString("full_name") != null ? rs.getString("full_name") : "N/A",
                    rs.getString("username"),
                    rs.getString("email") != null ? rs.getString("email") : "N/A",
                    rs.getString("phone_number") != null ? rs.getString("phone_number") : "N/A",
                    role.equals("Doctor") ? (rs.getString("specialization") != null ? rs.getString("specialization") : "Not specified") : null,
                    deleteBtn
                );
                users.add(userData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load " + role.toLowerCase() + "s");
        }
        
        table.setItems(users);
        
        if (addNewBtn != null) {
            vbox.getChildren().addAll(addNewBtn, table);
        } else {
            vbox.getChildren().add(table);
        }
        
        return vbox;
    }
    
    private void showAddUserDialog(String role, Stage parentStage) {
        try {
            // Prevent adding new Admin
            if (role.equals("Admin")) {
                showAlert("Not Allowed", "Only one Admin account is allowed in the system!");
                return;
            }
            
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(parentStage);
            dialog.setTitle("Add New " + role);
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            TextField nameField = new TextField();
            nameField.setPromptText("Full Name");
            nameField.setPrefWidth(250);
            
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            usernameField.setPrefWidth(250);
            
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            passwordField.setPrefWidth(250);
            
            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            emailField.setPrefWidth(250);
            
            TextField phoneField = new TextField();
            phoneField.setPromptText("Phone Number");
            phoneField.setPrefWidth(250);
            
            final TextField specializationField;
            if (role.equals("Doctor")) {
                specializationField = new TextField();
                specializationField.setPromptText("Specialization (e.g., Cardiologist)");
                specializationField.setPrefWidth(250);
            } else {
                specializationField = null;
            }
            
            int row = 0;
            grid.add(new Label("Full Name:*"), 0, row);
            grid.add(nameField, 1, row++);
            grid.add(new Label("Username:*"), 0, row);
            grid.add(usernameField, 1, row++);
            grid.add(new Label("Password:*"), 0, row);
            grid.add(passwordField, 1, row++);
            grid.add(new Label("Email:*"), 0, row);
            grid.add(emailField, 1, row++);
            grid.add(new Label("Phone:*"), 0, row);
            grid.add(phoneField, 1, row++);
            
            if (role.equals("Doctor")) {
                grid.add(new Label("Specialization:*"), 0, row);
                grid.add(specializationField, 1, row++);
            }
            
            Button saveBtn = new Button("Save");
            saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 20;");
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 20;");
            
            HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
            grid.add(buttonBox, 0, row, 2, 1);
            
            saveBtn.setOnAction(e -> {
                String name     = nameField.getText().trim();
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String email    = emailField.getText().trim();
                String phone    = phoneField.getText().trim();

                // Required: Full Name
                if (name.isEmpty()) {
                    showAlert("Missing Field", "Full Name is required!");
                    return;
                }

                // Required: Username (min 4 chars)
                if (username.isEmpty()) {
                    showAlert("Missing Field", "Username is required!");
                    return;
                }
                if (username.length() < 4) {
                    showAlert("Invalid Username", "Username must be at least 4 characters long.");
                    return;
                }

                // Required: Password (min 6 chars)
                if (password.isEmpty()) {
                    showAlert("Missing Field", "Password is required!");
                    return;
                }
                if (password.length() < 6) {
                    showAlert("Weak Password", "Password must be at least 6 characters long.");
                    return;
                }

                // Required: Email (must be valid format)
                if (email.isEmpty()) {
                    showAlert("Missing Field", "Email is required!");
                    return;
                }
                if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                    showAlert("Invalid Email", "Please enter a valid email address.\nExample: name@email.com");
                    return;
                }

                // Required: Phone (10-15 digits)
                if (phone.isEmpty()) {
                    showAlert("Missing Field", "Phone Number is required!");
                    return;
                }
                if (!phone.matches("^[0-9]{10,15}$")) {
                    showAlert("Invalid Phone Number", "Phone number must be 10–15 digits with no spaces or symbols.");
                    return;
                }

                // Doctor: Specialization required
                if (role.equals("Doctor") && specializationField != null && specializationField.getText().trim().isEmpty()) {
                    showAlert("Missing Field", "Specialization is required for Doctors!");
                    return;
                }

                // Duplicate username check
                String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Username Taken", "The username '" + username + "' already exists!\nPlease choose a different username.");
                        return;
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Database error: " + ex.getMessage());
                    return;
                }

                // Duplicate phone check
                String phoneCheckSql = "SELECT COUNT(*) FROM users WHERE phone_number = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement checkStmt = conn.prepareStatement(phoneCheckSql)) {
                    checkStmt.setString(1, phone);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Phone Number Taken", "The phone number '" + phone + "' is already registered.\nPlease use a different phone number.");
                        return;
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Database error: " + ex.getMessage());
                    return;
                }
                
                String sql = "INSERT INTO users (username, password, role, full_name, email, phone_number, specialization) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pst = conn.prepareStatement(sql)) {
                    
                    pst.setString(1, username);
                    pst.setString(2, password);
                    pst.setString(3, role);
                    pst.setString(4, name);
                    pst.setString(5, email);
                    pst.setString(6, phone);
                    
                    if (role.equals("Doctor") && specializationField != null) {
                        pst.setString(7, specializationField.getText().trim().isEmpty() ? null : specializationField.getText().trim());
                    } else {
                        pst.setNull(7, Types.VARCHAR);
                    }
                    
                    int result = pst.executeUpdate();
                    if (result > 0) {
                        showAlert("Success", role + " added successfully!");
                        dialog.close();
                        loadDashboardStats();
                        parentStage.close();
                        handleManageUsers();
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    if (ex.getMessage().contains("Duplicate entry")) {
                        showAlert("Error", "Username already exists!");
                    } else {
                        showAlert("Error", "Could not add user: " + ex.getMessage());
                    }
                }
            });
            
            cancelBtn.setOnAction(e -> dialog.close());
            
            Scene scene = new Scene(grid, 450, 400);
            dialog.setScene(scene);
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add user dialog: " + e.getMessage());
        }
    }
    
    private void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE userId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
            showAlert("Success", "User deleted successfully!");
            loadDashboardStats();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not delete user: " + e.getMessage());
        }
    }

    @FXML
    public void handleAppointmentRequest() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Pending Appointment Requests");
            stage.initModality(Modality.APPLICATION_MODAL);
            
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: white;");
            
            Label title = new Label("Pending Appointment Requests");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            
            TableView<PendingAppointmentData> table = new TableView<>();
            
            TableColumn<PendingAppointmentData, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setPrefWidth(50);
            
            TableColumn<PendingAppointmentData, String> patientCol = new TableColumn<>("Patient");
            patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
            patientCol.setPrefWidth(180);
            
            TableColumn<PendingAppointmentData, String> doctorCol = new TableColumn<>("Doctor");
            doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
            doctorCol.setPrefWidth(180);
            
            TableColumn<PendingAppointmentData, String> specializationCol = new TableColumn<>("Specialization");
            specializationCol.setCellValueFactory(new PropertyValueFactory<>("specialization"));
            specializationCol.setPrefWidth(150);
            
            TableColumn<PendingAppointmentData, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            dateCol.setPrefWidth(100);
            
            TableColumn<PendingAppointmentData, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
            timeCol.setPrefWidth(80);
            
            TableColumn<PendingAppointmentData, String> noteCol = new TableColumn<>("Health Note");
            noteCol.setCellValueFactory(new PropertyValueFactory<>("healthNote"));
            noteCol.setPrefWidth(200);
            
            TableColumn<PendingAppointmentData, Void> actionCol = new TableColumn<>("Action");
            actionCol.setPrefWidth(150);
            
            actionCol.setCellFactory(param -> new TableCell<PendingAppointmentData, Void>() {
                private final Button approveBtn = new Button("Approve");
                private final Button rejectBtn = new Button("Reject");
                private final HBox buttons = new HBox(5, approveBtn, rejectBtn);
                
                {
                    approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10;");
                    rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        PendingAppointmentData appointment = getTableView().getItems().get(getIndex());
                        approveBtn.setOnAction(e -> approvePendingAppointment(appointment, table));
                        rejectBtn.setOnAction(e -> rejectPendingAppointment(appointment, table));
                        setGraphic(buttons);
                    }
                }
            });
            
            table.getColumns().addAll(idCol, patientCol, doctorCol, specializationCol, dateCol, timeCol, noteCol, actionCol);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            ObservableList<PendingAppointmentData> pendingList = FXCollections.observableArrayList();
            
            String sql = "SELECT a.appointmentId, p.full_name as patientName, d.full_name as doctorName, " +
                        "d.specialization, a.appointmentDate, a.appointmentTime, a.description " +
                        "FROM appointments a " +
                        "JOIN users p ON a.patientId = p.userId " +
                        "JOIN users d ON a.doctorId = d.userId " +
                        "WHERE a.status = 'Pending' " +
                        "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC";
            
            try (Connection conn = DBConnection.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                
                while (rs.next()) {
                    Time sqlTime = rs.getTime("appointmentTime");
                    String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                    String specialization = rs.getString("specialization");
                    if (specialization == null || specialization.isEmpty()) {
                        specialization = "General";
                    }
                    
                    pendingList.add(new PendingAppointmentData(
                        rs.getInt("appointmentId"),
                        rs.getString("patientName"),
                        rs.getString("doctorName"),
                        specialization,
                        rs.getString("appointmentDate"),
                        timeStr,
                        rs.getString("description") != null ? rs.getString("description") : "No notes"
                    ));
                }
            }
            
            table.setItems(pendingList);
            vbox.getChildren().addAll(title, table);
            
            Label countLabel = new Label("Total Pending: " + pendingList.size());
            countLabel.setStyle("-fx-text-fill: #f39c12; -fx-padding: 10 0 0 0; -fx-font-weight: bold;");
            vbox.getChildren().add(countLabel);
            
            if (pendingList.isEmpty()) {
                Label noDataLabel = new Label("No pending appointment requests.");
                noDataLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 20;");
                vbox.getChildren().add(noDataLabel);
            }
            
            Scene scene = new Scene(vbox, 1100, 600);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load pending requests: " + e.getMessage());
        }
    }
    
    private void approvePendingAppointment(PendingAppointmentData appointment, TableView<PendingAppointmentData> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Approval");
        confirm.setHeaderText("Approve Appointment");
        confirm.setContentText("Approve appointment for:\n\n" +
                              "Patient: " + appointment.getPatientName() + "\n" +
                              "Doctor: " + appointment.getDoctorName() + "\n" +
                              "Date: " + appointment.getDate() + " at " + appointment.getTime());
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            String sql = "UPDATE appointments SET status = 'Approved' WHERE appointmentId = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, appointment.getId());
                pst.executeUpdate();
                
                showAlert("Success", "Appointment approved successfully!");
                table.getItems().remove(appointment);
                loadDashboardStats();
                
                if (table.getItems().isEmpty()) {
                    showAlert("Info", "No more pending appointments!");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Could not approve appointment: " + e.getMessage());
            }
        }
    }
    
    private void rejectPendingAppointment(PendingAppointmentData appointment, TableView<PendingAppointmentData> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Rejection");
        confirm.setHeaderText("Reject Appointment");
        confirm.setContentText("Are you sure you want to REJECT this appointment?\n\n" +
                              "Patient: " + appointment.getPatientName() + "\n" +
                              "Doctor: " + appointment.getDoctorName() + "\n" +
                              "Date: " + appointment.getDate() + " at " + appointment.getTime());
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            String sql = "UPDATE appointments SET status = 'Rejected' WHERE appointmentId = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, appointment.getId());
                pst.executeUpdate();
                
                showAlert("Success", "Appointment rejected successfully!");
                table.getItems().remove(appointment);
                loadDashboardStats();
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Could not reject appointment: " + e.getMessage());
            }
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
    
    public static class AppointmentData {
        private int id;
        private String patientName;
        private String doctorName;
        private String specialization;
        private String date;
        private String time;
        private String status;
        
        public AppointmentData(int id, String patientName, String doctorName, String specialization,
                              String date, String time, String status) {
            this.id = id;
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.specialization = specialization;
            this.date = date;
            this.time = time;
            this.status = status;
        }
        
        public int getId() { return id; }
        public String getPatientName() { return patientName; }
        public String getDoctorName() { return doctorName; }
        public String getSpecialization() { return specialization; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
    }
    
    public static class PendingAppointmentData {
        private int id;
        private String patientName;
        private String doctorName;
        private String specialization;
        private String date;
        private String time;
        private String healthNote;
        
        public PendingAppointmentData(int id, String patientName, String doctorName, String specialization,
                                     String date, String time, String healthNote) {
            this.id = id;
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.specialization = specialization;
            this.date = date;
            this.time = time;
            this.healthNote = healthNote;
        }
        
        public int getId() { return id; }
        public String getPatientName() { return patientName; }
        public String getDoctorName() { return doctorName; }
        public String getSpecialization() { return specialization; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getHealthNote() { return healthNote; }
    }
    
    public static class UserData {
        private int id;
        private String fullName;
        private String username;
        private String email;
        private String phone;
        private String specialization;
        private Button actionButton;
        
        public UserData(int id, String fullName, String username, String email, String phone,
                       String specialization, Button actionButton) {
            this.id = id;
            this.fullName = fullName;
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.specialization = specialization;
            this.actionButton = actionButton;
        }
        
        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getSpecialization() { return specialization; }
        public Button getActionButton() { return actionButton; }
    }
}