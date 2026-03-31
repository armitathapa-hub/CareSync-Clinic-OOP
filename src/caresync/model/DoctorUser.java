package caresync.model;

public class DoctorUser extends Person {
    private String specialization;
    private int experienceYears;
    
    public DoctorUser(int id, String fullName, String email, String specialization, int experienceYears) {
        super(id, fullName, email);
        this.specialization = specialization;
        this.experienceYears = experienceYears;
    }
    
    @Override
    public String getRole() {
        return "Doctor";
    }
    
    @Override
    public String getDashboardView() {
        return "/caresync/ui/doctordashboard.fxml";
    }
    
    public String getSpecialization() { return specialization; }
    public int getExperienceYears() { return experienceYears; }
}