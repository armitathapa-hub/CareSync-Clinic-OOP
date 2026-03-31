package caresync.model;

public class PatientUser extends Person {
    private String dateOfBirth;
    private String gender;
    
    public PatientUser(int id, String fullName, String email, String dateOfBirth, String gender) {
        super(id, fullName, email);
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }
    
    @Override
    public String getRole() {
        return "Patient";
    }
    
    @Override
    public String getDashboardView() {
        return "/caresync/ui/patientdashboard.fxml";
    }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
}