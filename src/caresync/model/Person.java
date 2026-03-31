package caresync.model;

public abstract class Person {
    private int id;
    private String fullName;
    private String email;
    
    public Person(int id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }
    
    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    
    public abstract String getRole();
    public abstract String getDashboardView();
}