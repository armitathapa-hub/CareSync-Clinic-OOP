package caresync.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            
            URL fxmlLocation = getClass().getResource("/caresync/ui/login.fxml");
            
            if (fxmlLocation == null) {
                System.err.println("ERROR: Still can't find login.fxml!");
                System.err.println("Trying alternative path...");
                
                fxmlLocation = getClass().getResource("caresync/ui/login.fxml");
            }
            
            if (fxmlLocation == null) {
                System.err.println("Failed to find FXML. Check package name is exactly 'caresync.ui'");
                return;
            }

            System.out.println("Found FXML at: " + fxmlLocation);
            
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setTitle("CareSync Clinic System");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setResizable(true);
            stage.show();

        } catch (Exception e) {
            System.err.println("Failed to load application:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}