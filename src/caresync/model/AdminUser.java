package caresync.model;

public class AdminUser extends Person {
    public AdminUser(int id, String fullName, String email) {
        super(id, fullName, email);
    }
    
    @Override
    public String getRole() {
        return "Admin";
    }
    
    @Override
    public String getDashboardView() {
        return "/caresync/ui/admindashboard.fxml";
    }
}