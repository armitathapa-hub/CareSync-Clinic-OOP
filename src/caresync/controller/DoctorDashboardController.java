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
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.*;

public class DoctorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label todayAppointments;
    @FXML private Label pendingAppointments;
    @FXML private Label totalPatients;
    @FXML private TableView<AppointmentData> appointmentsTable;
    @FXML private TableColumn<AppointmentData, Integer> colId;
    @FXML private TableColumn<AppointmentData, String> colPatient;
    @FXML private TableColumn<AppointmentData, String> colDate;
    @FXML private TableColumn<AppointmentData, String> colTime;
    @FXML private TableColumn<AppointmentData, String> colStatus;
    @FXML private TableColumn<AppointmentData, String> colDescription;
    @FXML private TableColumn<AppointmentData, Void> colAction;

    private int doctorId;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, Dr. " + UserSession.getLoggedInUserName() + "!");

        getDoctorId();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colAction.setCellFactory(param -> new TableCell<AppointmentData, Void>() {
            private final Button actionBtn = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppointmentData appointment = getTableView().getItems().get(getIndex());
                    String status = appointment.getStatus();

                    if (status.equals("Approved")) {
                        actionBtn.setText("Complete");
                        actionBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setOnAction(e -> updateAppointmentStatus(appointment.getId(), "Completed"));
                    } else if (status.equals("Completed")) {
                        actionBtn.setText("Completed");
                        actionBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setDisable(true);
                    } else {
                        actionBtn.setText("Pending");
                        actionBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setDisable(true);
                    }
                    setGraphic(actionBtn);
                }
            }
        });

        loadStats();
        loadAppointments();
    }

    private void getDoctorId() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT userId FROM users WHERE full_name = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, UserSession.getLoggedInUserName());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                doctorId = rs.getInt("userId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        try (Connection conn = DBConnection.getConnection()) {
            String todaySql = "SELECT COUNT(*) FROM appointments WHERE doctorId = ? AND appointmentDate = CURDATE()";
            PreparedStatement pst2 = conn.prepareStatement(todaySql);
            pst2.setInt(1, doctorId);
            ResultSet rs2 = pst2.executeQuery();
            if (rs2.next()) todayAppointments.setText(String.valueOf(rs2.getInt(1)));

            String pendingSql = "SELECT COUNT(*) FROM appointments WHERE doctorId = ? AND status = 'Pending'";
            PreparedStatement pst3 = conn.prepareStatement(pendingSql);
            pst3.setInt(1, doctorId);
            ResultSet rs3 = pst3.executeQuery();
            if (rs3.next()) pendingAppointments.setText(String.valueOf(rs3.getInt(1)));

            String patientsSql = "SELECT COUNT(DISTINCT patientId) FROM appointments WHERE doctorId = ?";
            PreparedStatement pst4 = conn.prepareStatement(patientsSql);
            pst4.setInt(1, doctorId);
            ResultSet rs4 = pst4.executeQuery();
            if (rs4.next()) totalPatients.setText(String.valueOf(rs4.getInt(1)));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load statistics");
        }
    }

    private void loadAppointments() {
        ObservableList<AppointmentData> appointments = FXCollections.observableArrayList();

        String sql = "SELECT a.appointmentId, u.full_name as patientName, a.appointmentDate, " +
                    "a.appointmentTime, a.status, a.description " +
                    "FROM appointments a JOIN users u ON a.patientId = u.userId " +
                    "WHERE a.doctorId = ? " +
                    "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, doctorId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Time sqlTime = rs.getTime("appointmentTime");
                String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                String description = rs.getString("description");
                if (description == null || description.isEmpty()) {
                    description = "-";
                }

                appointments.add(new AppointmentData(
                    rs.getInt("appointmentId"),
                    rs.getString("patientName"),
                    rs.getString("appointmentDate"),
                    timeStr,
                    rs.getString("status"),
                    description
                ));
            }

            appointmentsTable.setItems(appointments);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load appointments: " + e.getMessage());
        }
    }

    private void updateAppointmentStatus(int appointmentId, String newStatus) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Complete");
        confirm.setHeaderText("Complete Appointment");
        confirm.setContentText("Are you sure you want to mark this appointment as completed?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            String sql = "UPDATE appointments SET status = ? WHERE appointmentId = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, newStatus);
                pst.setInt(2, appointmentId);
                pst.executeUpdate();

                showAlert("Success", "Appointment marked as completed successfully!");
                loadStats();
                loadAppointments();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Could not update appointment: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadStats();
        loadAppointments();
        showAlert("Info", "Dashboard refreshed!");
    }

    @FXML
    public void handleAssignedAppointment() {
        showAppointmentsDialog("Today's Appointments", "appointmentDate = CURDATE()");
    }
    
    @FXML
    public void handleDailySchedules() {
        showAppointmentsDialog("Weekly Schedule (Next 7 Days)", "appointmentDate BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)");
    }
    
    private void showAppointmentsDialog(String title, String dateCondition) {
        Stage stage = new Stage();
        stage.setTitle(title + " - CareSync");
        stage.initModality(Modality.APPLICATION_MODAL);
        
        BorderPane mainPane = new BorderPane();
        mainPane.setStyle("-fx-background-color: #f4f7f6;");
        
        // Header
        VBox headerBox = new VBox(10);
        headerBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        Label headerTitle = new Label(title);
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label headerSubtitle = new Label("View and manage your scheduled appointments");
        headerSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        headerBox.getChildren().addAll(headerTitle, headerSubtitle);
        mainPane.setTop(headerBox);
        
        // TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: white; -fx-padding: 10;");
        
        // Tab 1: Appointments List
        Tab listTab = new Tab("Appointments");
        listTab.setClosable(false);
        VBox listContent = createAppointmentsListTab(title, dateCondition);
        listTab.setContent(listContent);
        
        // Tab 2: Statistics
        Tab statsTab = new Tab("Statistics");
        statsTab.setClosable(false);
        VBox statsContent = createStatisticsTab();
        statsTab.setContent(statsContent);
        
        // Tab 3: Patient Details
        Tab detailsTab = new Tab("Patient Details");
        detailsTab.setClosable(false);
        VBox detailsContent = createDetailsTab();
        detailsTab.setContent(detailsContent);
        
        tabPane.getTabs().addAll(listTab, statsTab, detailsTab);
        
        // Add selection listener to update details tab
        TableView<AppointmentData> listTable = (TableView<AppointmentData>) listContent.lookup("#appointmentsListTable");
        if (listTable != null) {
            listTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    updateDetailsTab(detailsContent, newSelection);
                }
            });
        }
        
        mainPane.setCenter(tabPane);
        
        // Bottom close button
        HBox bottomBox = new HBox();
        bottomBox.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        bottomBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        closeButton.setOnAction(e -> stage.close());
        bottomBox.getChildren().add(closeButton);
        mainPane.setBottom(bottomBox);
        
        Scene scene = new Scene(mainPane, 1000, 650);
        stage.setScene(scene);
        stage.showAndWait();
    }
    
    private VBox createAppointmentsListTab(String title, String dateCondition) {
        VBox vbox = new VBox(15);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Label listTitle = new Label("Appointments List");
        listTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        TableView<AppointmentData> table = new TableView<>();
        table.setId("appointmentsListTable");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<AppointmentData, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        
        TableColumn<AppointmentData, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        patientCol.setPrefWidth(200);
        
        TableColumn<AppointmentData, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);
        
        TableColumn<AppointmentData, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setPrefWidth(80);
        
        TableColumn<AppointmentData, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        TableColumn<AppointmentData, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(250);
        
        TableColumn<AppointmentData, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(100);
        
        actionCol.setCellFactory(param -> new TableCell<AppointmentData, Void>() {
            private final Button actionBtn = new Button();
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppointmentData appointment = getTableView().getItems().get(getIndex());
                    String status = appointment.getStatus();
                    
                    if (status.equals("Approved")) {
                        actionBtn.setText("Complete");
                        actionBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setOnAction(e -> {
                            updateAppointmentStatus(appointment.getId(), "Completed");
                            getTableView().refresh();
                            loadStats();
                        });
                    } else if (status.equals("Completed")) {
                        actionBtn.setText("✓ Done");
                        actionBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setDisable(true);
                    } else {
                        actionBtn.setText("Pending");
                        actionBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                        actionBtn.setDisable(true);
                    }
                    setGraphic(actionBtn);
                }
            }
        });
        
        // Status cell factory with colors
        statusCol.setCellFactory(col -> new TableCell<AppointmentData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Pending":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "Approved":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "Completed":
                            setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        table.getColumns().addAll(idCol, patientCol, dateCol, timeCol, statusCol, descCol, actionCol);
        
        // Load data based on date condition
        ObservableList<AppointmentData> appointments = FXCollections.observableArrayList();
        
        String sql = "SELECT a.appointmentId, u.full_name as patientName, a.appointmentDate, " +
                    "a.appointmentTime, a.status, a.description " +
                    "FROM appointments a JOIN users u ON a.patientId = u.userId " +
                    "WHERE a.doctorId = ? AND " + dateCondition + " " +
                    "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, doctorId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Time sqlTime = rs.getTime("appointmentTime");
                String timeStr = (sqlTime != null) ? sqlTime.toString().substring(0, 5) : "N/A";
                String description = rs.getString("description");
                if (description == null || description.isEmpty()) {
                    description = "-";
                }
                
                appointments.add(new AppointmentData(
                    rs.getInt("appointmentId"),
                    rs.getString("patientName"),
                    rs.getString("appointmentDate"),
                    timeStr,
                    rs.getString("status"),
                    description
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load appointments: " + e.getMessage());
        }
        
        table.setItems(appointments);
        
        Label infoLabel = new Label("Total: " + appointments.size() + " appointments | Click on any appointment to view patient details");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-padding: 10 0 0 0;");
        
        vbox.getChildren().addAll(listTitle, table, infoLabel);
        
        return vbox;
    }
    
    private VBox createStatisticsTab() {
        VBox vbox = new VBox(20);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        int pending = 0, approved = 0, completed = 0, rejected = 0;
        
        for (AppointmentData app : appointmentsTable.getItems()) {
            String status = app.getStatus();
            switch (status) {
                case "Pending": pending++; break;
                case "Approved": approved++; break;
                case "Completed": completed++; break;
                case "Rejected": rejected++; break;
            }
        }
        
        int total = appointmentsTable.getItems().size();
        
        Label statsTitle = new Label("Appointment Statistics");
        statsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Statistics cards grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(15);
        statsGrid.setStyle("-fx-padding: 20 0;");
        
        statsGrid.add(createStatCard("Total Appointments", String.valueOf(total), "#3498db"), 0, 0);
        statsGrid.add(createStatCard("Pending", String.valueOf(pending), "#f39c12"), 1, 0);
        statsGrid.add(createStatCard("Approved", String.valueOf(approved), "#27ae60"), 2, 0);
        statsGrid.add(createStatCard("Completed", String.valueOf(completed), "#2c3e50"), 0, 1);
        statsGrid.add(createStatCard("Rejected", String.valueOf(rejected), "#e74c3c"), 1, 1);
        
        // Progress bars
        VBox progressBox = new VBox(15);
        progressBox.setStyle("-fx-padding: 20 0 0 0;");
        Label progressTitle = new Label("Appointment Progress");
        progressTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        progressBox.getChildren().add(progressTitle);
        
        if (total > 0) {
            progressBox.getChildren().add(createProgressBar("Pending", (pending * 100.0) / total, "#f39c12"));
            progressBox.getChildren().add(createProgressBar("Approved", (approved * 100.0) / total, "#27ae60"));
            progressBox.getChildren().add(createProgressBar("Completed", (completed * 100.0) / total, "#2c3e50"));
        } else {
            Label noDataLabel = new Label("No appointments to display statistics");
            noDataLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 20;");
            progressBox.getChildren().add(noDataLabel);
        }
        
        vbox.getChildren().addAll(statsTitle, statsGrid, progressBox);
        
        return vbox;
    }
    
    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; " +
                      "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPrefWidth(180);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }
    
    private VBox createProgressBar(String label, double percentage, String color) {
        VBox vbox = new VBox(5);
        HBox labelBox = new HBox();
        labelBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label progressLabel = new Label(label + " (" + String.format("%.1f", percentage) + "%)");
        progressLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        labelBox.getChildren().add(progressLabel);
        
        ProgressBar progressBar = new ProgressBar(percentage / 100);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + color + ";");
        
        vbox.getChildren().addAll(labelBox, progressBar);
        return vbox;
    }
    
    private VBox createDetailsTab() {
        VBox vbox = new VBox(15);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Label detailsTitle = new Label("Patient Details");
        detailsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label instructionLabel = new Label("Select an appointment from the 'Appointments' tab to view patient details");
        instructionLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
        
        vbox.getChildren().addAll(detailsTitle, instructionLabel);
        
        return vbox;
    }
    
    private void updateDetailsTab(VBox detailsContent, AppointmentData selected) {
        detailsContent.getChildren().clear();
        
        Label detailsTitle = new Label("Patient Details");
        detailsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        try {
            String sql = "SELECT u.userId, u.full_name, u.email, u.phone_number, u.date_of_birth, u.gender, " +
                         "a.appointmentId, a.appointmentDate, a.appointmentTime, a.status, a.description " +
                         "FROM appointments a " +
                         "JOIN users u ON a.patientId = u.userId " +
                         "WHERE a.appointmentId = ? AND a.doctorId = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                
                pst.setInt(1, selected.getId());
                pst.setInt(2, doctorId);
                ResultSet rs = pst.executeQuery();
                
                if (rs.next()) {
                    GridPane detailsGrid = new GridPane();
                    detailsGrid.setHgap(15);
                    detailsGrid.setVgap(12);
                    detailsGrid.setStyle("-fx-padding: 20;");
                    
                    int row = 0;
                    
                    addDetailRow(detailsGrid, "Patient Name:", rs.getString("full_name"), row++);
                    addDetailRow(detailsGrid, "Email:", rs.getString("email") != null ? rs.getString("email") : "N/A", row++);
                    addDetailRow(detailsGrid, "Phone:", rs.getString("phone_number") != null ? rs.getString("phone_number") : "N/A", row++);
                    addDetailRow(detailsGrid, "Date of Birth:", rs.getString("date_of_birth") != null ? rs.getString("date_of_birth") : "N/A", row++);
                    addDetailRow(detailsGrid, "Gender:", rs.getString("gender") != null ? rs.getString("gender") : "N/A", row++);
                    addDetailRow(detailsGrid, "Appointment Date:", rs.getString("appointmentDate"), row++);
                    addDetailRow(detailsGrid, "Appointment Time:", rs.getString("appointmentTime"), row++);
                    
                    String status = rs.getString("status");
                    String statusColor;
                    switch (status) {
                        case "Pending": statusColor = "#f39c12"; break;
                        case "Approved": statusColor = "#27ae60"; break;
                        case "Completed": statusColor = "#2c3e50"; break;
                        default: statusColor = "#e74c3c";
                    }
                    addColoredDetailRow(detailsGrid, "Status:", status, statusColor, row++);
                    
                    // Health Notes section
                    Label notesLabel = new Label("Health Notes:");
                    notesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                    detailsGrid.add(notesLabel, 0, row);
                    
                    TextArea notesArea = new TextArea(rs.getString("description") != null ? rs.getString("description") : "No notes provided");
                    notesArea.setEditable(false);
                    notesArea.setWrapText(true);
                    notesArea.setPrefHeight(100);
                    notesArea.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
                    detailsGrid.add(notesArea, 1, row);
                    
                    detailsContent.getChildren().addAll(detailsTitle, detailsGrid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Could not load patient details: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            detailsContent.getChildren().addAll(detailsTitle, errorLabel);
        }
    }
    
    private void addDetailRow(GridPane grid, String label, String value, int row) {
        Label labelField = new Label(label);
        labelField.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label valueField = new Label(value);
        valueField.setStyle("-fx-text-fill: #7f8c8d;");
        grid.add(labelField, 0, row);
        grid.add(valueField, 1, row);
    }
    
    private void addColoredDetailRow(GridPane grid, String label, String value, String color, int row) {
        Label labelField = new Label(label);
        labelField.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label valueField = new Label(value);
        valueField.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        grid.add(labelField, 0, row);
        grid.add(valueField, 1, row);
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
        private String date;
        private String time;
        private String status;
        private String description;

        public AppointmentData(int id, String patientName, String date, String time, String status, String description) {
            this.id = id;
            this.patientName = patientName;
            this.date = date;
            this.time = time;
            this.status = status;
            this.description = description;
        }

        public int getId() { return id; }
        public String getPatientName() { return patientName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}