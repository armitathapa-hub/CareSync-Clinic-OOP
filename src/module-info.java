module CareSync {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires mysql.connector.j;

    opens caresync.main to javafx.graphics;
    opens caresync.controller to javafx.fxml;
    opens caresync.model to javafx.base;
    
    exports caresync.main;
    exports caresync.controller;
    exports caresync.database;
    exports caresync.model;
}