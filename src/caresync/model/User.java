package caresync.model;

public class User {
    private int userId;
    private String username;
    private String role;
    private String fullName;  // Optional: add more fields if needed

    public User(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
    
    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}