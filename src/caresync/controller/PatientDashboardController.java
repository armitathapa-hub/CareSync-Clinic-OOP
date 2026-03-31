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

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AppointmentModel> appointmentTable;
    @FXML private TableColumn<AppointmentModel, Integer> colId;
    @FXML private TableColumn<AppointmentModel, String> colDoctor;
    @FXML private TableColumn<AppointmentModel, String> colDate;
    @FXML private TableColumn<AppointmentModel, String> colTime;
    @FXML private TableColumn<AppointmentModel, String> colStatus;
    
    @FXML private Button bookAppointmentBtn;
    @FXML private Button viewStatusBtn;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + UserSession.getLoggedInUserName() + "!");

        colId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        appointmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadAppointments();
        
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
                     "WHERE a.patientId = ? ORDER BY a.appointmentDate DESC";

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
            
            if (list.isEmpty()) {
                Label noDataLabel = new Label("No appointments found. Click Book Appointment to schedule one.");
                noDataLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 20;");
                appointmentTable.setPlaceholder(noDataLabel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load appointments: " + e.getMessage());
        }
    }
    
    @FXML
    public void viewAppointmentStatus() {
        loadAppointments();
        
        // Create a new stage for the appointment status window
        Stage statusStage = new Stage();
        statusStage.setTitle("My Appointments - CareSync");
        statusStage.initModality(Modality.APPLICATION_MODAL);
        
        // Main container
        BorderPane mainPane = new BorderPane();
        mainPane.setStyle("-fx-background-color: #f4f7f6;");
        
        // Header
        VBox headerBox = new VBox(10);
        headerBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        Label headerTitle = new Label("My Appointments Dashboard");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label headerSubtitle = new Label("View and manage your appointment history");
        headerSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        headerBox.getChildren().addAll(headerTitle, headerSubtitle);
        mainPane.setTop(headerBox);
        
        // Center with TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: white; -fx-padding: 10;");
        
        // Tab 1: Statistics Dashboard
        Tab statsTab = new Tab("Statistics");
        statsTab.setClosable(false);
        VBox statsContent = createStatisticsTab();
        statsTab.setContent(statsContent);
        
        // Tab 2: All Appointments List
        Tab listTab = new Tab("All Appointments");
        listTab.setClosable(false);
        VBox listContent = createAppointmentsListTab();
        listTab.setContent(listContent);
        
        // Tab 3: Appointment Details
        Tab detailsTab = new Tab("Appointment Details");
        detailsTab.setClosable(false);
        VBox detailsContent = createDetailsTab();
        detailsTab.setContent(detailsContent);
        
        tabPane.getTabs().addAll(statsTab, listTab, detailsTab);
        
        // Add selection listener to update details tab when appointment is selected in list tab
        TableView<AppointmentModel> listTable = (TableView<AppointmentModel>) listContent.lookup("#appointmentsListTable");
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
        closeButton.setOnAction(e -> statusStage.close());
        bottomBox.getChildren().add(closeButton);
        mainPane.setBottom(bottomBox);
        
        Scene scene = new Scene(mainPane, 900, 650);
        statusStage.setScene(scene);
        statusStage.showAndWait();
    }
    
    private VBox createStatisticsTab() {
        VBox vbox = new VBox(20);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        int pending = 0, approved = 0, completed = 0, rejected = 0;
        
        for (AppointmentModel app : appointmentTable.getItems()) {
            String status = app.getStatus();
            switch (status) {
                case "Pending": pending++; break;
                case "Approved": approved++; break;
                case "Completed": completed++; break;
                case "Rejected": rejected++; break;
                case "Cancelled": rejected++; break;
            }
        }
        
        int total = appointmentTable.getItems().size();
        
        // Title
        Label statsTitle = new Label("Appointment Statistics");
        statsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Statistics cards grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(15);
        statsGrid.setStyle("-fx-padding: 20 0;");
        
        // Total Card
        VBox totalCard = createStatCard("Total Appointments", String.valueOf(total), "#3498db");
        statsGrid.add(totalCard, 0, 0);
        
        // Pending Card
        VBox pendingCard = createStatCard("Pending", String.valueOf(pending), "#f39c12");
        statsGrid.add(pendingCard, 1, 0);
        
        // Approved Card
        VBox approvedCard = createStatCard("Approved", String.valueOf(approved), "#27ae60");
        statsGrid.add(approvedCard, 2, 0);
        
        // Completed Card
        VBox completedCard = createStatCard("Completed", String.valueOf(completed), "#2c3e50");
        statsGrid.add(completedCard, 0, 1);
        
        // Rejected Card
        VBox rejectedCard = createStatCard("Rejected", String.valueOf(rejected), "#e74c3c");
        statsGrid.add(rejectedCard, 1, 1);
        
        // Progress indicators
        VBox progressBox = new VBox(15);
        progressBox.setStyle("-fx-padding: 20 0 0 0;");
        Label progressTitle = new Label("Appointment Progress");
        progressTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        progressBox.getChildren().add(progressTitle);
        
        if (total > 0) {
            double pendingPercent = (pending * 100.0) / total;
            double approvedPercent = (approved * 100.0) / total;
            double completedPercent = (completed * 100.0) / total;
            
            progressBox.getChildren().add(createProgressBar("Pending", pendingPercent, "#f39c12"));
            progressBox.getChildren().add(createProgressBar("Approved", approvedPercent, "#27ae60"));
            progressBox.getChildren().add(createProgressBar("Completed", completedPercent, "#2c3e50"));
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
    
    private VBox createAppointmentsListTab() {
        VBox vbox = new VBox(15);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Label listTitle = new Label("All Appointments");
        listTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        TableView<AppointmentModel> table = new TableView<>();
        table.setId("appointmentsListTable");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<AppointmentModel, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        idCol.setPrefWidth(60);
        
        TableColumn<AppointmentModel, String> doctorCol = new TableColumn<>("Doctor");
        doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        doctorCol.setPrefWidth(200);
        
        TableColumn<AppointmentModel, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);
        
        TableColumn<AppointmentModel, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setPrefWidth(80);
        
        TableColumn<AppointmentModel, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        // Status cell factory with colors
        statusCol.setCellFactory(col -> new TableCell<AppointmentModel, String>() {
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
        
        table.getColumns().addAll(idCol, doctorCol, dateCol, timeCol, statusCol);
        
        ObservableList<AppointmentModel> appointments = FXCollections.observableArrayList(appointmentTable.getItems());
        table.setItems(appointments);
        
        Label infoLabel = new Label("Click on any appointment to view details in the Details tab");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-padding: 10 0 0 0;");
        
        vbox.getChildren().addAll(listTitle, table, infoLabel);
        
        return vbox;
    }
    
    private VBox createDetailsTab() {
        VBox vbox = new VBox(15);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Label detailsTitle = new Label("Appointment Details");
        detailsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label instructionLabel = new Label("Select an appointment from the 'All Appointments' tab to view details");
        instructionLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
        
        vbox.getChildren().addAll(detailsTitle, instructionLabel);
        
        return vbox;
    }
    
    private void updateDetailsTab(VBox detailsContent, AppointmentModel selected) {
        detailsContent.getChildren().clear();
        
        Label detailsTitle = new Label("Appointment Details");
        detailsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        try {
            String sql = "SELECT a.appointmentId, a.appointmentDate, a.appointmentTime, a.status, a.description, " +
                         "d.full_name as doctorName, d.specialization, d.email as doctorEmail, d.phone_number as doctorPhone " +
                         "FROM appointments a " +
                         "JOIN users d ON a.doctorId = d.userId " +
                         "WHERE a.appointmentId = ? AND a.patientId = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                
                pst.setInt(1, selected.getAppointmentId());
                pst.setInt(2, UserSession.getLoggedInUserId());
                ResultSet rs = pst.executeQuery();
                
                if (rs.next()) {
                    GridPane detailsGrid = new GridPane();
                    detailsGrid.setHgap(15);
                    detailsGrid.setVgap(12);
                    detailsGrid.setStyle("-fx-padding: 20;");
                    
                    int row = 0;
                    
                    addDetailRow(detailsGrid, "Appointment ID:", String.valueOf(rs.getInt("appointmentId")), row++);
                    addDetailRow(detailsGrid, "Doctor:", rs.getString("doctorName"), row++);
                    addDetailRow(detailsGrid, "Specialization:", rs.getString("specialization") != null ? rs.getString("specialization") : "General", row++);
                    addDetailRow(detailsGrid, "Date:", rs.getString("appointmentDate"), row++);
                    addDetailRow(detailsGrid, "Time:", rs.getString("appointmentTime"), row++);
                    
                    String status = rs.getString("status");
                    String statusColor;
                    switch (status) {
                        case "Pending": statusColor = "#f39c12"; break;
                        case "Approved": statusColor = "#27ae60"; break;
                        case "Completed": statusColor = "#2c3e50"; break;
                        default: statusColor = "#e74c3c";
                    }
                    addColoredDetailRow(detailsGrid, "Status:", status, statusColor, row++);
                    
                    addDetailRow(detailsGrid, "Doctor Email:", rs.getString("doctorEmail") != null ? rs.getString("doctorEmail") : "N/A", row++);
                    addDetailRow(detailsGrid, "Doctor Phone:", rs.getString("doctorPhone") != null ? rs.getString("doctorPhone") : "N/A", row++);
                    
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
            Label errorLabel = new Label("Could not load details: " + e.getMessage());
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
    public void openBookingModal() {
        try {
            String[] possiblePaths = {
                "/caresync/ui/booking_modal.fxml",
                "/caresync/ui/bookingmodal.fxml",
                "/caresync/ui/BookingModal.fxml"
            };

            FXMLLoader loader = null;
            Parent root = null;

            for (String path : possiblePaths) {
                if (getClass().getResource(path) != null) {
                    loader = new FXMLLoader(getClass().getResource(path));
                    root = loader.load();
                    break;
                }
            }

            if (root == null) {
                showAlert("Error", "Could not find booking_modal.fxml!");
                return;
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Book New Appointment");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAppointments();

        } catch (Exception e) {
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